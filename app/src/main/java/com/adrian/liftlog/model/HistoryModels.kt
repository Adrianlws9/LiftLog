package com.adrian.liftlog.model

/** One exercise's sets, as displayed on the workout detail/history screen. */
data class ExerciseHistory(
    val exerciseName: String,
    val sets: List<LoggedSet>
)

/** A full completed workout with all its exercises and sets, for display. */
data class WorkoutHistoryDetail(
    val dayName: String,
    val title: String,
    val dateEpochMillis: Long,
    val durationSeconds: Long?,
    val notes: String?,
    val exercises: List<ExerciseHistory>
)

data class PersonalBest(
    val maxWeightKg: Double?,
    val maxWeightLb: Double?,
    val maxReps: Int?
)

data class MonthlySummary(
    val workoutCount: Int,
    val totalDurationSeconds: Long,
    val totalSets: Int,
    val monthLabel: String,
    val totalVolumeKg: Double,
    val mostFrequentExercise: String?,
    val workoutCountVsLastMonth: Int?,
    val bodyWeightChangeKg: Double?
)