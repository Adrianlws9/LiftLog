package com.adrian.liftlog.model

/**
 * How a weight is expressed for a given set.
 * BAR_ONLY is a distinct option (not just "0 kg") because it has real
 * meaning in the gym — you're lifting the empty barbell, not zero weight.
 */
enum class WeightUnit {
    KG,
    LB,
    BAR_ONLY
}

/**
 * One logged set for one exercise: how much weight, in what unit, for
 * how many reps. Weight is nullable because it's meaningless when
 * unit == BAR_ONLY.
 */
data class LoggedSet(
    val weight: Double?,
    val unit: WeightUnit,
    val reps: Int
)

/**
 * One exercise as performed during an active/completed workout —
 * its name plus the list of sets logged for it.
 */
data class LoggedExercise(
    val exerciseName: String,
    val sets: MutableList<LoggedSet> = mutableListOf()
)

/** Formats a set for display, e.g. "40 kg × 10" or "BAR ONLY × 8". */
fun LoggedSet.displayString(): String {
    val repsPart = "× $reps"
    return when (unit) {
        WeightUnit.KG -> "${formatWeightNumber(weight)} kg $repsPart"
        WeightUnit.LB -> "${formatWeightNumber(weight)} lb $repsPart"
        WeightUnit.BAR_ONLY -> "BAR ONLY $repsPart"
    }
}

/** Formats 40.0 as "40" but keeps 37.5 as "37.5" — avoids ugly trailing zeros. */
private fun formatWeightNumber(weight: Double?): String {
    if (weight == null) return "0"
    return if (weight == weight.toLong().toDouble()) {
        weight.toLong().toString()
    } else {
        weight.toString()
    }
}

/** Formats seconds as "45 min" or "1 hr 3 min" — never raw minutes over 60. */
fun formatDuration(totalSeconds: Long): String {
    val totalMinutes = totalSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "$minutes min"
        minutes == 0L -> "$hours hr"
        else -> "$hours hr $minutes min"
    }
}

/** Converts a weight to kilograms, regardless of its original unit. */
fun Double.toKg(unit: WeightUnit): Double {
    return when (unit) {
        WeightUnit.LB -> this * 0.453592
        else -> this
    }
}
