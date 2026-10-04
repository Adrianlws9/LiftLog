package com.adrian.liftlog.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * One row per completed (or in-progress) workout session.
 */
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateEpochMillis: Long,
    val dayName: String,
    val title: String,
    val isCompleted: Boolean = false,
    val durationSeconds: Long? = null,
    val notes: String? = null
)

/**
 * One row per exercise performed within a specific workout.
 * `workoutId` links back to the WorkoutEntity it belongs to.
 */
@Entity(
    tableName = "logged_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LoggedExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long,
    val exerciseName: String,
    val orderIndex: Int   // preserves the order exercises were displayed in
)

/**
 * One row per logged set. `exerciseId` links back to the
 * LoggedExerciseEntity it belongs to.
 */
@Entity(
    tableName = "logged_sets",
    foreignKeys = [
        ForeignKey(
            entity = LoggedExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LoggedSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    val setIndex: Int,      // 1st set, 2nd set, etc.
    val weight: Double?,
    val unit: String,       // stores WeightUnit as text, e.g. "KG", "LB", "BAR_ONLY"
    val reps: Int
)

/**
 * One row per day of the week in the user's workout plan.
 * dayOfWeek uses Calendar constants (Calendar.MONDAY = 2, etc.) so it
 * lines up naturally with how we already determine "today."
 */
@Entity(tableName = "plan_days")
data class PlanDayEntity(
    @PrimaryKey
    val dayOfWeek: Int,
    val title: String,
    val isRestDay: Boolean
)

/**
 * One row per exercise within a plan day. dayOfWeek links back to
 * PlanDayEntity — changing this value is how an exercise "moves" to
 * a different day.
 */
@Entity(
    tableName = "plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = PlanDayEntity::class,
            parentColumns = ["dayOfWeek"],
            childColumns = ["dayOfWeek"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlanExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: Int,
    val name: String,
    val orderIndex: Int
)

/**
 * One row per body-weight check-in. Separate from workout data entirely —
 * tracked over time independent of any specific workout.
 */
@Entity(tableName = "body_weight_entries")
data class BodyWeightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateEpochMillis: Long,
    val weight: Double,
    val unit: String // "KG" or "LB" — bar-only doesn't apply here
)

/** Result shape for a logged-exercise row joined with its parent workout's date. */
data class LoggedExerciseWithDate(
    val id: Long,
    val workoutId: Long,
    val exerciseName: String,
    val orderIndex: Int,
    val workoutDate: Long
)

data class ExerciseFrequency(
    val exerciseName: String,
    val cnt: Int
)