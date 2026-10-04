package com.adrian.liftlog.model

import java.util.Calendar

/**
 * Hardcoded default weekly workout plan.
 * This will later be replaced by data stored in the Room database,
 * but for now it lets us build the UI quickly.
 */
object DefaultWorkoutPlan {

    private val monday = DayWorkout(
        dayName = "MONDAY",
        title = "Legs — Quads + Calves + Shoulders",
        isRestDay = false,
        exercises = listOf(
            Exercise("Leg Press (Low Placement)"),
            Exercise("Barbell Squats"),
            Exercise("Leg Extensions"),
            Exercise("Standing Calf Raises"),
            Exercise("Dumbbell Press"),
            Exercise("Lateral Raises"),
            Exercise("Rear Flys"),
            Exercise("Front Raises")
        )
    )

    private val tuesday = DayWorkout(
        dayName = "TUESDAY",
        title = "Upper — Back + Triceps",
        isRestDay = false,
        exercises = listOf(
            Exercise("Assisted Pullups"),
            Exercise("Lat Pulldown"),
            Exercise("Seated Rows"),
            Exercise("Face Pulls"),
            Exercise("Assisted Dips"),
            Exercise("Triceps Pushdown"),
            Exercise("Overhead Triceps Extensions"),
            Exercise("Dumbbell Shrugs")
        )
    )

    private val wednesday = DayWorkout(
        dayName = "WEDNESDAY",
        title = "Rest Day",
        isRestDay = true,
        exercises = emptyList()
    )

    private val thursday = DayWorkout(
        dayName = "THURSDAY",
        title = "Legs — Hamstrings + Glutes + Core",
        isRestDay = false,
        exercises = listOf(
            Exercise("Leg Press (High Placement)"),
            Exercise("Hip Thrusts"),
            Exercise("Leg Curls"),
            Exercise("Romanian Dead Lifts"),
            Exercise("Hanging Knee Raises"),
            Exercise("Abdominal"),
            Exercise("Back Extensions")
        )
    )

    private val friday = DayWorkout(
        dayName = "FRIDAY",
        title = "Upper — Back + Triceps",
        isRestDay = false,
        exercises = listOf(
            Exercise("Assisted Pullups"),
            Exercise("Lat Pulldown"),
            Exercise("Seated Rows"),
            Exercise("Face Pulls"),
            Exercise("Assisted Dips"),
            Exercise("Triceps Pushdown"),
            Exercise("Overhead Triceps Extensions"),
            Exercise("Dumbbell Shrugs")
        )
    )

    private val saturday = DayWorkout(
        dayName = "SATURDAY",
        title = "Rest Day",
        isRestDay = true,
        exercises = emptyList()
    )

    private val sunday = DayWorkout(
        dayName = "SUNDAY",
        title = "Rest Day",
        isRestDay = true,
        exercises = emptyList()
    )

    /**
     * Returns the DayWorkout for a given Calendar.DAY_OF_WEEK value
     * (Calendar.SUNDAY = 1, Calendar.MONDAY = 2, ... Calendar.SATURDAY = 7).
     */
    fun getWorkoutFor(dayOfWeek: Int): DayWorkout {
        return when (dayOfWeek) {
            Calendar.MONDAY -> monday
            Calendar.TUESDAY -> tuesday
            Calendar.WEDNESDAY -> wednesday
            Calendar.THURSDAY -> thursday
            Calendar.FRIDAY -> friday
            Calendar.SATURDAY -> saturday
            Calendar.SUNDAY -> sunday
            else -> sunday // fallback, shouldn't happen
        }
    }

    /** Convenience function: returns today's workout automatically. */
    fun getTodaysWorkout(): DayWorkout {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return getWorkoutFor(today)
    }

    fun dayNameFor(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "MONDAY"
            Calendar.TUESDAY -> "TUESDAY"
            Calendar.WEDNESDAY -> "WEDNESDAY"
            Calendar.THURSDAY -> "THURSDAY"
            Calendar.FRIDAY -> "FRIDAY"
            Calendar.SATURDAY -> "SATURDAY"
            Calendar.SUNDAY -> "SUNDAY"
            else -> "UNKNOWN"
        }
    }
}