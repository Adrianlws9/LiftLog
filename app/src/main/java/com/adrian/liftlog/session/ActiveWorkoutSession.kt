package com.adrian.liftlog.session

import com.adrian.liftlog.model.LoggedExercise

/**
 * Holds the workout that is currently in progress, independent of any
 * screen. Lives for as long as the app process does, so the user can
 * leave WorkoutActivity, browse other screens, and come back.
 */
object ActiveWorkoutSession {

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

    fun start(dayName: String, title: String, exerciseNames: List<String>) {
        this.dayName = dayName
        this.title = title
        this.startTimeMillis = System.currentTimeMillis()
        this.notes = null
        exercises.clear()
        exerciseNames.forEach { exercises.add(LoggedExercise(exerciseName = it)) }
        isActive = true
    }

    fun elapsedSeconds(): Long {
        return if (isActive) (System.currentTimeMillis() - startTimeMillis) / 1000 else 0L
    }

    fun clear() {
        isActive = false
        exercises.clear()
        notes = null
    }
}