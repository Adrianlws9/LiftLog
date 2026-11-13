package com.adrian.liftlog.session

import android.content.Context
import com.adrian.liftlog.model.LoggedExercise
import com.adrian.liftlog.model.LoggedSet
import com.adrian.liftlog.model.WeightUnit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Holds the workout that is currently in progress, independent of any
 * screen. Also mirrors itself to disk (a "draft") so an in-progress
 * workout survives the app process being killed.
 */
object ActiveWorkoutSession {

    private const val PREFS = "active_workout_draft"
    private const val KEY = "draft_json"

    private var appContext: Context? = null

    var isActive: Boolean = false
        private set

    var dayName: String = ""
        private set
    var title: String = ""
        private set
    var startTimeMillis: Long = 0L
        private set

    val exercises: MutableList<LoggedExercise> = mutableListOf()
    var notes: String? = null

    /** Call once from Application.onCreate. Also restores any saved draft. */
    fun init(context: Context) {
        appContext = context.applicationContext
        if (!isActive) restoreDraft()
    }

    fun start(dayName: String, title: String, exerciseNames: List<String>) {
        this.dayName = dayName
        this.title = title
        this.startTimeMillis = System.currentTimeMillis()
        this.notes = null
        exercises.clear()
        exerciseNames.forEach { exercises.add(LoggedExercise(exerciseName = it)) }
        isActive = true
        saveDraft()
    }

    fun elapsedSeconds(): Long {
        return if (isActive) (System.currentTimeMillis() - startTimeMillis) / 1000 else 0L
    }

    fun clear() {
        isActive = false
        exercises.clear()
        notes = null
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
    }

    // ---------- Draft persistence ----------

    fun saveDraft() {
        val ctx = appContext ?: return
        if (!isActive) return

        val root = JSONObject()
        root.put("dayName", dayName)
        root.put("title", title)
        root.put("startTimeMillis", startTimeMillis)
        root.put("notes", notes ?: JSONObject.NULL)

        val exArray = JSONArray()
        for (ex in exercises) {
            val exObj = JSONObject()
            exObj.put("name", ex.exerciseName)
            val setsArray = JSONArray()
            for (s in ex.sets) {
                val sObj = JSONObject()
                sObj.put("weight", s.weight ?: JSONObject.NULL)
                sObj.put("unit", s.unit.name)
                sObj.put("reps", s.reps)
                setsArray.put(sObj)
            }
            exObj.put("sets", setsArray)
            exArray.put(exObj)
        }
        root.put("exercises", exArray)

        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, root.toString()).apply()
    }

    private fun restoreDraft() {
        val ctx = appContext ?: return
        val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return
        try {
            val root = JSONObject(json)
            dayName = root.getString("dayName")
            title = root.getString("title")
            startTimeMillis = root.getLong("startTimeMillis")
            notes = if (root.isNull("notes")) null else root.getString("notes")

            exercises.clear()
            val exArray = root.getJSONArray("exercises")
            for (i in 0 until exArray.length()) {
                val exObj = exArray.getJSONObject(i)
                val sets = mutableListOf<LoggedSet>()
                val setsArray = exObj.getJSONArray("sets")
                for (j in 0 until setsArray.length()) {
                    val sObj = setsArray.getJSONObject(j)
                    sets.add(
                        LoggedSet(
                            weight = if (sObj.isNull("weight")) null else sObj.getDouble("weight"),
                            unit = WeightUnit.valueOf(sObj.getString("unit")),
                            reps = sObj.getInt("reps")
                        )
                    )
                }
                exercises.add(LoggedExercise(exObj.getString("name"), sets))
            }
            isActive = true
        } catch (e: Exception) {
            // A corrupt draft must never crash the app: just discard it.
            clear()
        }
    }
}