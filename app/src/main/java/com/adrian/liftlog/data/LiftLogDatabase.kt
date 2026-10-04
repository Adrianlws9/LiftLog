package com.adrian.liftlog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workouts ADD COLUMN durationSeconds INTEGER")
        db.execSQL("ALTER TABLE workouts ADD COLUMN notes TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS plan_days (
                dayOfWeek INTEGER NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                isRestDay INTEGER NOT NULL
            )
            """
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS plan_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dayOfWeek INTEGER NOT NULL,
                name TEXT NOT NULL,
                orderIndex INTEGER NOT NULL,
                FOREIGN KEY (dayOfWeek) REFERENCES plan_days(dayOfWeek) ON DELETE CASCADE
            )
            """
        )

        // Seed with the current real plan (Calendar.DAY_OF_WEEK: SUNDAY=1 ... SATURDAY=7)
        val days = listOf(
            Triple(2, "Legs — Quads + Calves + Shoulders", false), // Monday
            Triple(3, "Upper — Back + Triceps", false),            // Tuesday
            Triple(4, "Rest Day", true),                            // Wednesday
            Triple(5, "Legs — Hamstrings + Glutes + Core", false), // Thursday
            Triple(6, "Upper — Back + Triceps", false),            // Friday
            Triple(7, "Rest Day", true),                            // Saturday
            Triple(1, "Rest Day", true)                             // Sunday
        )
        for ((dayOfWeek, title, isRest) in days) {
            db.execSQL(
                "INSERT INTO plan_days (dayOfWeek, title, isRestDay) VALUES (?, ?, ?)",
                arrayOf(dayOfWeek, title, if (isRest) 1 else 0)
            )
        }

        val mondayExercises = listOf(
            "Leg Press (Low Placement)", "Barbell Squats", "Leg Extensions",
            "Standing Calf Raises", "Dumbbell Press", "Lateral Raises", "Rear Flys", "Front Raises"
        )
        val tuesdayFridayExercises = listOf(
            "Assisted Pullups", "Lat Pulldown", "Seated Rows", "Face Pulls",
            "Assisted Dips", "Triceps Pushdown", "Overhead Triceps Extensions", "Dumbbell Shrugs"
        )
        val thursdayExercises = listOf(
            "Leg Press (High Placement)", "Hip Thrusts", "Leg Curls",
            "Romanian Dead Lifts", "Hanging Knee Raises", "Abdominal", "Back Extensions"
        )

        fun insertExercises(dayOfWeek: Int, names: List<String>) {
            names.forEachIndexed { index, name ->
                db.execSQL(
                    "INSERT INTO plan_exercises (dayOfWeek, name, orderIndex) VALUES (?, ?, ?)",
                    arrayOf(dayOfWeek, name, index)
                )
            }
        }

        insertExercises(2, mondayExercises)
        insertExercises(3, tuesdayFridayExercises)
        insertExercises(5, thursdayExercises)
        insertExercises(6, tuesdayFridayExercises)
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The previous migration created plan_exercises with a schema
        // mismatch (id column missing an explicit NOT NULL). Rebuild it
        // correctly — this table only ever held auto-seeded plan data,
        // never irreplaceable user history, so it's safe to recreate.
        db.execSQL("DROP TABLE IF EXISTS plan_exercises")
        db.execSQL(
            """
            CREATE TABLE plan_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dayOfWeek INTEGER NOT NULL,
                name TEXT NOT NULL,
                orderIndex INTEGER NOT NULL,
                FOREIGN KEY (dayOfWeek) REFERENCES plan_days(dayOfWeek) ON DELETE CASCADE
            )
            """
        )

        val mondayExercises = listOf(
            "Leg Press (Low Placement)", "Barbell Squats", "Leg Extensions",
            "Standing Calf Raises", "Dumbbell Press", "Lateral Raises", "Rear Flys", "Front Raises"
        )
        val tuesdayFridayExercises = listOf(
            "Assisted Pullups", "Lat Pulldown", "Seated Rows", "Face Pulls",
            "Assisted Dips", "Triceps Pushdown", "Overhead Triceps Extensions", "Dumbbell Shrugs"
        )
        val thursdayExercises = listOf(
            "Leg Press (High Placement)", "Hip Thrusts", "Leg Curls",
            "Romanian Dead Lifts", "Hanging Knee Raises", "Abdominal", "Back Extensions"
        )

        fun insertExercises(dayOfWeek: Int, names: List<String>) {
            names.forEachIndexed { index, name ->
                db.execSQL(
                    "INSERT INTO plan_exercises (dayOfWeek, name, orderIndex) VALUES (?, ?, ?)",
                    arrayOf(dayOfWeek, name, index)
                )
            }
        }

        insertExercises(2, mondayExercises)
        insertExercises(3, tuesdayFridayExercises)
        insertExercises(5, thursdayExercises)
        insertExercises(6, tuesdayFridayExercises)
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS body_weight_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dateEpochMillis INTEGER NOT NULL,
                weight REAL NOT NULL,
                unit TEXT NOT NULL
            )
            """
        )
    }
}

@Database(
    entities = [
        WorkoutEntity::class,
        LoggedExerciseEntity::class,
        LoggedSetEntity::class,
        PlanDayEntity::class,
        PlanExerciseEntity::class,
        BodyWeightEntity::class
    ],
    version = 5,
    exportSchema = true
)

abstract class LiftLogDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun loggedExerciseDao(): LoggedExerciseDao
    abstract fun loggedSetDao(): LoggedSetDao
    abstract fun planDao(): PlanDao
    abstract fun bodyWeightDao(): BodyWeightDao

    companion object {
        @Volatile
        private var INSTANCE: LiftLogDatabase? = null

        fun getInstance(context: Context): LiftLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LiftLogDatabase::class.java,
                    "liftlog_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedDefaultPlan(db)
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Seeds the default weekly plan. Runs only when the database is
         * created fresh (new install) — existing installs already have
         * this data from the v2->v3 migration and must not be touched here.
         */
        private fun seedDefaultPlan(db: SupportSQLiteDatabase) {
            val days = listOf(
                Triple(2, "Legs — Quads + Calves + Shoulders", false),
                Triple(3, "Upper — Back + Triceps", false),
                Triple(4, "Rest Day", true),
                Triple(5, "Legs — Hamstrings + Glutes + Core", false),
                Triple(6, "Upper — Back + Triceps", false),
                Triple(7, "Rest Day", true),
                Triple(1, "Rest Day", true)
            )
            for ((dayOfWeek, title, isRest) in days) {
                db.execSQL(
                    "INSERT INTO plan_days (dayOfWeek, title, isRestDay) VALUES (?, ?, ?)",
                    arrayOf(dayOfWeek, title, if (isRest) 1 else 0)
                )
            }

            val mondayExercises = listOf(
                "Leg Press (Low Placement)", "Barbell Squats", "Leg Extensions",
                "Standing Calf Raises", "Dumbbell Press", "Lateral Raises", "Rear Flys", "Front Raises"
            )
            val tuesdayFridayExercises = listOf(
                "Assisted Pullups", "Lat Pulldown", "Seated Rows", "Face Pulls",
                "Assisted Dips", "Triceps Pushdown", "Overhead Triceps Extensions", "Dumbbell Shrugs"
            )
            val thursdayExercises = listOf(
                "Leg Press (High Placement)", "Hip Thrusts", "Leg Curls",
                "Romanian Dead Lifts", "Hanging Knee Raises", "Abdominal", "Back Extensions"
            )

            fun insertExercises(dayOfWeek: Int, names: List<String>) {
                names.forEachIndexed { index, name ->
                    db.execSQL(
                        "INSERT INTO plan_exercises (dayOfWeek, name, orderIndex) VALUES (?, ?, ?)",
                        arrayOf(dayOfWeek, name, index)
                    )
                }
            }

            insertExercises(2, mondayExercises)
            insertExercises(3, tuesdayFridayExercises)
            insertExercises(5, thursdayExercises)
            insertExercises(6, tuesdayFridayExercises)
        }
    }
}