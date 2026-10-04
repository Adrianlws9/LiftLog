package com.adrian.liftlog.data

import android.content.Context
import com.adrian.liftlog.model.LoggedExercise
import androidx.lifecycle.asLiveData
import com.adrian.liftlog.model.ExerciseHistory
import com.adrian.liftlog.model.LoggedSet
import com.adrian.liftlog.model.WeightUnit
import com.adrian.liftlog.model.WorkoutHistoryDetail
import com.adrian.liftlog.model.PersonalBest
import com.adrian.liftlog.model.MonthlySummary
import com.adrian.liftlog.model.toKg

class WorkoutRepository(context: Context) {

    private val database = LiftLogDatabase.getInstance(context)

    /**
     * Saves a completed workout and all its exercises/sets to the database.
     * Returns the new workout's database id.
     */
    suspend fun saveCompletedWorkout(
        dayName: String,
        title: String,
        exercises: List<LoggedExercise>,
        durationSeconds: Long,
        notes: String?
    ): Long {
        val workoutId = database.workoutDao().insertWorkout(
            WorkoutEntity(
                dateEpochMillis = System.currentTimeMillis(),
                dayName = dayName,
                title = title,
                isCompleted = true,
                durationSeconds = durationSeconds,
                notes = notes
            )
        )

        for ((exerciseOrder, exercise) in exercises.withIndex()) {
            val exerciseId = database.loggedExerciseDao().insertExercise(
                LoggedExerciseEntity(
                    workoutId = workoutId,
                    exerciseName = exercise.exerciseName,
                    orderIndex = exerciseOrder
                )
            )

            for ((setPosition, set) in exercise.sets.withIndex()) {
                database.loggedSetDao().insertSet(
                    LoggedSetEntity(
                        exerciseId = exerciseId,
                        setIndex = setPosition + 1,
                        weight = set.weight,
                        unit = set.unit.name,
                        reps = set.reps
                    )
                )
            }
        }

        return workoutId
    }

    suspend fun getMonthlySummary(monthOffset: Int = 0): MonthlySummary {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, monthOffset)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis
        val monthLabel = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(cal.time)
        cal.add(java.util.Calendar.MONTH, 1)
        val startOfNextMonth = cal.timeInMillis

        val workouts = database.workoutDao().getWorkoutsInRange(startOfMonth, startOfNextMonth)

        var totalDuration = 0L
        var totalSets = 0
        for (workout in workouts) {
            totalDuration += workout.durationSeconds ?: 0L
            val exercises = database.loggedExerciseDao().getExercisesForWorkout(workout.id)
            for (exercise in exercises) {
                totalSets += database.loggedSetDao().getSetsForExercise(exercise.id).size
            }
        }

        val setsThisMonth = database.loggedSetDao().getSetsInRange(startOfMonth, startOfNextMonth)
        var totalVolumeKg = 0.0
        for (set in setsThisMonth) {
            if (set.weight != null && set.unit != "BAR_ONLY") {
                val kg = set.weight.toKg(WeightUnit.valueOf(set.unit))
                totalVolumeKg += kg * set.reps
            }
        }

        val mostFrequent = database.loggedExerciseDao()
            .getMostFrequentExerciseInRange(startOfMonth, startOfNextMonth)?.exerciseName

        // Compare to last month's workout count
        val lastMonthCal = java.util.Calendar.getInstance()
        lastMonthCal.timeInMillis = startOfMonth
        lastMonthCal.add(java.util.Calendar.MONTH, -1)
        val startOfLastMonth = lastMonthCal.timeInMillis
        val lastMonthWorkouts = database.workoutDao().getWorkoutsInRange(startOfLastMonth, startOfMonth)
        val workoutCountDelta = workouts.size - lastMonthWorkouts.size

        val weightEntries = database.bodyWeightDao().getEntriesInRange(startOfMonth, startOfNextMonth)
        val weightChange = if (weightEntries.size >= 2) {
            val first = weightEntries.first()
            val last = weightEntries.last()
            val firstKg = first.weight.toKg(WeightUnit.valueOf(first.unit))
            val lastKg = last.weight.toKg(WeightUnit.valueOf(last.unit))
            lastKg - firstKg
        } else null

        return MonthlySummary(
            workoutCount = workouts.size,
            totalDurationSeconds = totalDuration,
            totalSets = totalSets,
            monthLabel = monthLabel,
            totalVolumeKg = totalVolumeKg,
            mostFrequentExercise = mostFrequent,
            workoutCountVsLastMonth = if (lastMonthWorkouts.isNotEmpty() || workouts.isNotEmpty()) workoutCountDelta else null,
            bodyWeightChangeKg = weightChange
        )
    }

    /** Live-updating list of all saved workouts, newest first. */
    fun getAllWorkoutsLiveData() = database.workoutDao().getAllWorkouts().asLiveData()

    /** Fetches one workout plus all its exercises and sets, assembled for display. */
    suspend fun getWorkoutDetail(workoutId: Long): WorkoutHistoryDetail? {
        val workout = database.workoutDao().getWorkoutById(workoutId) ?: return null
        val exerciseEntities = database.loggedExerciseDao().getExercisesForWorkout(workoutId)

        val exerciseHistories = exerciseEntities.map { exerciseEntity ->
            val setEntities = database.loggedSetDao().getSetsForExercise(exerciseEntity.id)
            val sets = setEntities.map { setEntity ->
                LoggedSet(
                    weight = setEntity.weight,
                    unit = WeightUnit.valueOf(setEntity.unit),
                    reps = setEntity.reps
                )
            }
            ExerciseHistory(exerciseName = exerciseEntity.exerciseName, sets = sets)
        }

        return WorkoutHistoryDetail(
            dayName = workout.dayName,
            title = workout.title,
            dateEpochMillis = workout.dateEpochMillis,
            durationSeconds = workout.durationSeconds,
            notes = workout.notes,
            exercises = exerciseHistories
        )
    }

    /**
     * For each given exercise name, finds the most recent previous workout
     * that included it and returns its sets. Exercises with no prior
     * history simply won't appear in the returned map.
     */
    suspend fun getLastPerformances(
        exerciseNames: List<String>,
        excludeWorkoutId: Long
    ): Map<String, List<LoggedSet>> {
        val result = mutableMapOf<String, List<LoggedSet>>()

        for (name in exerciseNames) {
            val exerciseEntity = database.loggedExerciseDao()
                .getMostRecentExerciseByName(name, excludeWorkoutId)
            if (exerciseEntity != null) {
                val setEntities = database.loggedSetDao().getSetsForExercise(exerciseEntity.id)
                val sets = setEntities.map { setEntity ->
                    LoggedSet(
                        weight = setEntity.weight,
                        unit = WeightUnit.valueOf(setEntity.unit),
                        reps = setEntity.reps
                    )
                }
                result[name] = sets
            }
        }

        return result
    }
    suspend fun updateWorkout(
        workoutId: Long,
        title: String,
        notes: String?,
        exercises: List<LoggedExercise>
    ) {
        database.workoutDao().updateWorkoutDetails(workoutId, title, notes)
        database.loggedExerciseDao().deleteExercisesForWorkout(workoutId)

        for ((exerciseOrder, exercise) in exercises.withIndex()) {
            val exerciseId = database.loggedExerciseDao().insertExercise(
                LoggedExerciseEntity(
                    workoutId = workoutId,
                    exerciseName = exercise.exerciseName,
                    orderIndex = exerciseOrder
                )
            )

            for ((setPosition, set) in exercise.sets.withIndex()) {
                database.loggedSetDao().insertSet(
                    LoggedSetEntity(
                        exerciseId = exerciseId,
                        setIndex = setPosition + 1,
                        weight = set.weight,
                        unit = set.unit.name,
                        reps = set.reps
                    )
                )
            }
        }
    }

    suspend fun getMostRecentNotes(title: String): String? = database.workoutDao().getMostRecentNotes(title)

    /** Fetches a workout's exercises/sets in editable (LoggedExercise) form, for the edit screen. */
    suspend fun getWorkoutForEditing(workoutId: Long): List<LoggedExercise> {
        val exerciseEntities = database.loggedExerciseDao().getExercisesForWorkout(workoutId)

        return exerciseEntities.map { exerciseEntity ->
            val setEntities = database.loggedSetDao().getSetsForExercise(exerciseEntity.id)
            val sets = setEntities.map { setEntity ->
                LoggedSet(
                    weight = setEntity.weight,
                    unit = WeightUnit.valueOf(setEntity.unit),
                    reps = setEntity.reps
                )
            }.toMutableList()
            LoggedExercise(exerciseName = exerciseEntity.exerciseName, sets = sets)
        }
    }
    suspend fun deleteWorkout(workoutId: Long) {
        database.workoutDao().deleteWorkoutById(workoutId)
    }

    suspend fun getPlanDay(dayOfWeek: Int): com.adrian.liftlog.data.PlanDayEntity? {
        return database.planDao().getAllDays().find { it.dayOfWeek == dayOfWeek }
    }

    suspend fun getAllPlanDays(): List<com.adrian.liftlog.data.PlanDayEntity> {
        return database.planDao().getAllDays()
    }

    suspend fun getExercisesForDay(dayOfWeek: Int): List<com.adrian.liftlog.data.PlanExerciseEntity> {
        return database.planDao().getExercisesForDay(dayOfWeek)
    }

    suspend fun updatePlanDay(dayOfWeek: Int, title: String, isRestDay: Boolean) {
        database.planDao().updateDay(dayOfWeek, title, isRestDay)
    }

    suspend fun addPlanExercise(dayOfWeek: Int, name: String) {
        val existing = database.planDao().getExercisesForDay(dayOfWeek)
        database.planDao().insertExercise(
            com.adrian.liftlog.data.PlanExerciseEntity(
                dayOfWeek = dayOfWeek, name = name, orderIndex = existing.size
            )
        )
    }

    suspend fun deletePlanExercise(exerciseId: Long) {
        database.planDao().deleteExercise(exerciseId)
    }

    suspend fun renamePlanExercise(exerciseId: Long, name: String) {
        database.planDao().renameExercise(exerciseId, name)
    }

    suspend fun movePlanExercise(exerciseId: Long, newDayOfWeek: Int) {
        val existing = database.planDao().getExercisesForDay(newDayOfWeek)
        database.planDao().moveExercise(exerciseId, newDayOfWeek, existing.size)
    }

    suspend fun reorderPlanExercises(dayOfWeek: Int, orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            database.planDao().updateExerciseOrder(id, index)
        }
    }

    suspend fun getPersonalBests(exerciseNames: List<String>): Map<String, PersonalBest> {
        val result = mutableMapOf<String, PersonalBest>()
        for (name in exerciseNames) {
            val maxKg = database.loggedSetDao().getMaxWeightForExercise(name, "KG")
            val maxLb = database.loggedSetDao().getMaxWeightForExercise(name, "LB")
            val maxReps = database.loggedSetDao().getMaxRepsForExercise(name)
            result[name] = PersonalBest(maxKg, maxLb, maxReps)
        }
        return result
    }

    suspend fun getAllWorkoutsForExport(): List<WorkoutHistoryDetail> {
        val workouts = database.workoutDao().getAllWorkouts()
        // getAllWorkouts() returns a Flow; for a one-time export we need a snapshot.
        // We'll instead query the raw list directly:
        return exportAllWorkouts()
    }

    private suspend fun exportAllWorkouts(): List<WorkoutHistoryDetail> {
        val workoutEntities = database.workoutDao().getAllWorkoutsSnapshot()
        return workoutEntities.map { workout ->
            val exerciseEntities = database.loggedExerciseDao().getExercisesForWorkout(workout.id)
            val exerciseHistories = exerciseEntities.map { exerciseEntity ->
                val sets = database.loggedSetDao().getSetsForExercise(exerciseEntity.id).map { setEntity ->
                    LoggedSet(
                        weight = setEntity.weight,
                        unit = WeightUnit.valueOf(setEntity.unit),
                        reps = setEntity.reps
                    )
                }
                ExerciseHistory(exerciseName = exerciseEntity.exerciseName, sets = sets)
            }
            WorkoutHistoryDetail(
                dayName = workout.dayName,
                title = workout.title,
                dateEpochMillis = workout.dateEpochMillis,
                durationSeconds = workout.durationSeconds,
                notes = workout.notes,
                exercises = exerciseHistories
            )
        }
    }

    suspend fun getFullPlanForExport(): List<Pair<PlanDayEntity, List<PlanExerciseEntity>>> {
        val days = database.planDao().getAllDays()
        return days.map { day -> day to database.planDao().getExercisesForDay(day.dayOfWeek) }
    }

    // ---- EXPORT ----

    suspend fun exportAllDataAsJson(): String {
        val workouts = exportAllWorkouts()
        val plan = getFullPlanForExport()

        val root = org.json.JSONObject()
        root.put("exportVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val workoutsArray = org.json.JSONArray()
        for (w in workouts) {
            val wObj = org.json.JSONObject()
            wObj.put("dayName", w.dayName)
            wObj.put("title", w.title)
            wObj.put("dateEpochMillis", w.dateEpochMillis)
            wObj.put("durationSeconds", w.durationSeconds ?: org.json.JSONObject.NULL)
            wObj.put("notes", w.notes ?: org.json.JSONObject.NULL)

            val exercisesArray = org.json.JSONArray()
            for (ex in w.exercises) {
                val exObj = org.json.JSONObject()
                exObj.put("exerciseName", ex.exerciseName)
                val setsArray = org.json.JSONArray()
                for (set in ex.sets) {
                    val setObj = org.json.JSONObject()
                    setObj.put("weight", set.weight ?: org.json.JSONObject.NULL)
                    setObj.put("unit", set.unit.name)
                    setObj.put("reps", set.reps)
                    setsArray.put(setObj)
                }
                exObj.put("sets", setsArray)
                exercisesArray.put(exObj)
            }
            wObj.put("exercises", exercisesArray)
            workoutsArray.put(wObj)
        }
        root.put("workouts", workoutsArray)

        val planArray = org.json.JSONArray()
        for ((day, exercises) in plan) {
            val dayObj = org.json.JSONObject()
            dayObj.put("dayOfWeek", day.dayOfWeek)
            dayObj.put("title", day.title)
            dayObj.put("isRestDay", day.isRestDay)
            val exArray = org.json.JSONArray()
            for (ex in exercises) {
                val exObj = org.json.JSONObject()
                exObj.put("name", ex.name)
                exObj.put("orderIndex", ex.orderIndex)
                exArray.put(exObj)
            }
            dayObj.put("exercises", exArray)
            planArray.put(dayObj)
        }
        root.put("plan", planArray)

        return root.toString(2)
    }

    // ---- IMPORT ----

    suspend fun importAllDataFromJson(json: String): Pair<Int, Int> {
        val root = org.json.JSONObject(json)

        var workoutsImported = 0
        var exercisesImported = 0

        val workoutsArray = root.optJSONArray("workouts") ?: org.json.JSONArray()
        for (i in 0 until workoutsArray.length()) {
            val wObj = workoutsArray.getJSONObject(i)
            val workoutId = database.workoutDao().insertWorkout(
                WorkoutEntity(
                    dateEpochMillis = wObj.getLong("dateEpochMillis"),
                    dayName = wObj.getString("dayName"),
                    title = wObj.getString("title"),
                    isCompleted = true,
                    durationSeconds = if (wObj.isNull("durationSeconds")) null else wObj.getLong("durationSeconds"),
                    notes = if (wObj.isNull("notes")) null else wObj.getString("notes")
                )
            )
            workoutsImported++

            val exercisesArray = wObj.getJSONArray("exercises")
            for (j in 0 until exercisesArray.length()) {
                val exObj = exercisesArray.getJSONObject(j)
                val exerciseId = database.loggedExerciseDao().insertExercise(
                    LoggedExerciseEntity(
                        workoutId = workoutId,
                        exerciseName = exObj.getString("exerciseName"),
                        orderIndex = j
                    )
                )
                exercisesImported++

                val setsArray = exObj.getJSONArray("sets")
                for (k in 0 until setsArray.length()) {
                    val setObj = setsArray.getJSONObject(k)
                    database.loggedSetDao().insertSet(
                        LoggedSetEntity(
                            exerciseId = exerciseId,
                            setIndex = k + 1,
                            weight = if (setObj.isNull("weight")) null else setObj.getDouble("weight"),
                            unit = setObj.getString("unit"),
                            reps = setObj.getInt("reps")
                        )
                    )
                }
            }
        }

        val planArray = root.optJSONArray("plan") ?: org.json.JSONArray()
        for (i in 0 until planArray.length()) {
            val dayObj = planArray.getJSONObject(i)
            val dayOfWeek = dayObj.getInt("dayOfWeek")
            database.planDao().updateDay(
                dayOfWeek, dayObj.getString("title"), dayObj.getBoolean("isRestDay")
            )
            database.planDao().deleteExercisesForDay(dayOfWeek)

            val exArray = dayObj.getJSONArray("exercises")
            for (j in 0 until exArray.length()) {
                val exObj = exArray.getJSONObject(j)
                database.planDao().insertExercise(
                    PlanExerciseEntity(
                        dayOfWeek = dayOfWeek,
                        name = exObj.getString("name"),
                        orderIndex = exObj.getInt("orderIndex")
                    )
                )
            }
        }

        return Pair(workoutsImported, exercisesImported)
    }

    fun getAllBodyWeightEntriesLiveData() = database.bodyWeightDao().getAll().asLiveData()

    suspend fun addBodyWeightEntry(weight: Double, unit: String) {
        database.bodyWeightDao().insert(
            BodyWeightEntity(
                dateEpochMillis = System.currentTimeMillis(),
                weight = weight,
                unit = unit
            )
        )
    }

    suspend fun deleteBodyWeightEntry(id: Long) {
        database.bodyWeightDao().delete(id)
    }

    /**
     * For a given exercise, returns its best set's weight (in kg) per
     * workout session over time — one point per session, using the
     * heaviest set logged that day. Used for progress charting.
     */
    suspend fun getExerciseProgressInKg(exerciseName: String): List<Pair<Long, Double>> {
        val instances = database.loggedExerciseDao().getAllInstancesOfExercise(exerciseName)
        val result = mutableListOf<Pair<Long, Double>>()

        for (instance in instances) {
            val sets = database.loggedSetDao().getSetsForExercise(instance.id)
            val bestKg = sets
                .filter { it.weight != null && it.unit != "BAR_ONLY" }
                .maxOfOrNull { setEntity ->
                    setEntity.weight!!.toKg(WeightUnit.valueOf(setEntity.unit))
                }
            if (bestKg != null) {
                result.add(instance.workoutDate to bestKg)
            }
        }
        return result
    }

    suspend fun getAllExerciseNames(): List<String> = database.loggedExerciseDao().getAllDistinctExerciseNames()
}