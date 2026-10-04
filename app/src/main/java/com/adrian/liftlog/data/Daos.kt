package com.adrian.liftlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Query("SELECT * FROM workouts ORDER BY dateEpochMillis DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: Long): WorkoutEntity?

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkoutById(workoutId: Long)

    @Query("UPDATE workouts SET title = :title, notes = :notes WHERE id = :workoutId")
    suspend fun updateWorkoutDetails(workoutId: Long, title: String, notes: String?)

    @Query("SELECT notes FROM workouts WHERE title = :title AND notes IS NOT NULL AND notes != '' ORDER BY dateEpochMillis DESC LIMIT 1")
    suspend fun getMostRecentNotes(title: String): String?

    @Query("SELECT * FROM workouts WHERE dateEpochMillis >= :startOfMonth AND dateEpochMillis < :startOfNextMonth")
    suspend fun getWorkoutsInRange(startOfMonth: Long, startOfNextMonth: Long): List<WorkoutEntity>

    @Query("SELECT * FROM workouts ORDER BY dateEpochMillis ASC")
    suspend fun getAllWorkoutsSnapshot(): List<WorkoutEntity>
}

@Dao
interface LoggedExerciseDao {

    @Insert
    suspend fun insertExercise(exercise: LoggedExerciseEntity): Long

    @Query("SELECT * FROM logged_exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
    suspend fun getExercisesForWorkout(workoutId: Long): List<LoggedExerciseEntity>

    @Query("DELETE FROM logged_exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesForWorkout(workoutId: Long)

    @Query(
        """
        SELECT le.* FROM logged_exercises le
        INNER JOIN workouts w ON le.workoutId = w.id
        WHERE le.exerciseName = :exerciseName AND w.id != :excludeWorkoutId
        ORDER BY w.dateEpochMillis DESC
        LIMIT 1
        """
    )
    suspend fun getMostRecentExerciseByName(
        exerciseName: String,
        excludeWorkoutId: Long
    ): LoggedExerciseEntity?

    @Query(
        """
        SELECT le.*, w.dateEpochMillis as workoutDate FROM logged_exercises le
        INNER JOIN workouts w ON le.workoutId = w.id
        WHERE le.exerciseName = :exerciseName
        ORDER BY w.dateEpochMillis ASC
        """
    )
    suspend fun getAllInstancesOfExercise(exerciseName: String): List<LoggedExerciseWithDate>

    @Query("SELECT DISTINCT exerciseName FROM logged_exercises ORDER BY exerciseName ASC")
    suspend fun getAllDistinctExerciseNames(): List<String>

    @Query(
        """
        SELECT le.exerciseName, COUNT(*) as cnt FROM logged_exercises le
        INNER JOIN workouts w ON le.workoutId = w.id
        WHERE w.dateEpochMillis >= :start AND w.dateEpochMillis < :end
        GROUP BY le.exerciseName
        ORDER BY cnt DESC
        LIMIT 1
        """
    )
    suspend fun getMostFrequentExerciseInRange(start: Long, end: Long): ExerciseFrequency?
}

@Dao
interface LoggedSetDao {

    @Insert
    suspend fun insertSet(set: LoggedSetEntity): Long

    @Query("SELECT * FROM logged_sets WHERE exerciseId = :exerciseId ORDER BY setIndex ASC")
    suspend fun getSetsForExercise(exerciseId: Long): List<LoggedSetEntity>

    @Query(
        """
        SELECT MAX(weight) FROM logged_sets
        WHERE exerciseId IN (SELECT id FROM logged_exercises WHERE exerciseName = :exerciseName)
        AND unit = :unit
        """
    )
    suspend fun getMaxWeightForExercise(exerciseName: String, unit: String): Double?

    @Query(
        """
        SELECT MAX(reps) FROM logged_sets
        WHERE exerciseId IN (SELECT id FROM logged_exercises WHERE exerciseName = :exerciseName)
        """
    )
    suspend fun getMaxRepsForExercise(exerciseName: String): Int?

    @Query(
        """
        SELECT ls.* FROM logged_sets ls
        INNER JOIN logged_exercises le ON ls.exerciseId = le.id
        INNER JOIN workouts w ON le.workoutId = w.id
        WHERE w.dateEpochMillis >= :start AND w.dateEpochMillis < :end
        """
    )
    suspend fun getSetsInRange(start: Long, end: Long): List<LoggedSetEntity>

}

@Dao
interface PlanDao {

    @Query("SELECT * FROM plan_days ORDER BY dayOfWeek ASC")
    suspend fun getAllDays(): List<PlanDayEntity>

    @Query("SELECT * FROM plan_exercises WHERE dayOfWeek = :dayOfWeek ORDER BY orderIndex ASC")
    suspend fun getExercisesForDay(dayOfWeek: Int): List<PlanExerciseEntity>

    @Query("UPDATE plan_days SET title = :title, isRestDay = :isRestDay WHERE dayOfWeek = :dayOfWeek")
    suspend fun updateDay(dayOfWeek: Int, title: String, isRestDay: Boolean)

    @Insert
    suspend fun insertExercise(exercise: PlanExerciseEntity): Long

    @Query("DELETE FROM plan_exercises WHERE id = :exerciseId")
    suspend fun deleteExercise(exerciseId: Long)

    @Query("UPDATE plan_exercises SET name = :name WHERE id = :exerciseId")
    suspend fun renameExercise(exerciseId: Long, name: String)

    @Query("UPDATE plan_exercises SET dayOfWeek = :newDayOfWeek, orderIndex = :orderIndex WHERE id = :exerciseId")
    suspend fun moveExercise(exerciseId: Long, newDayOfWeek: Int, orderIndex: Int)

    @Query("UPDATE plan_exercises SET orderIndex = :orderIndex WHERE id = :exerciseId")
    suspend fun updateExerciseOrder(exerciseId: Long, orderIndex: Int)

    @Query("DELETE FROM plan_exercises WHERE dayOfWeek = :dayOfWeek")
    suspend fun deleteExercisesForDay(dayOfWeek: Int)
}

@Dao
interface BodyWeightDao {

    @Insert
    suspend fun insert(entry: BodyWeightEntity): Long

    @Query("SELECT * FROM body_weight_entries ORDER BY dateEpochMillis DESC")
    fun getAll(): Flow<List<BodyWeightEntity>>

    @Query("DELETE FROM body_weight_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM body_weight_entries WHERE dateEpochMillis >= :start AND dateEpochMillis < :end ORDER BY dateEpochMillis ASC")
    suspend fun getEntriesInRange(start: Long, end: Long): List<BodyWeightEntity>
}