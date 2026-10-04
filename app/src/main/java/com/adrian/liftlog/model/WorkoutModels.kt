package com.adrian.liftlog.model

/**
 * Represents a single exercise inside a day's workout plan,
 * e.g. "Leg Press".
 */
data class Exercise(
    val name: String
)

/**
 * Represents the workout scheduled for one day of the week.
 * If isRestDay is true, the exercises list will be empty.
 */
data class DayWorkout(
    val dayName: String,       // e.g. "MONDAY"
    val title: String,         // e.g. "Legs — Quads + Calves"
    val isRestDay: Boolean,
    val exercises: List<Exercise>
)