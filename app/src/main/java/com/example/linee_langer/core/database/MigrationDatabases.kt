package com.example.linee_langer.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.impl.Migration_3_4

object MigrationDatabases {

    val MIGRATION_1_2 = object : Migration(1,2){
        override fun migrate(db: SupportSQLiteDatabase){
            db.execSQL("""
            CREATE TABLE IF NOT EXISTS `LangerLineEntity` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `analysisId` INTEGER NOT NULL, 
                `startX` REAL NOT NULL, 
                `startY` REAL NOT NULL, 
                `endX` REAL NOT NULL, 
                `endY` REAL NOT NULL, 
                `intensity` REAL NOT NULL,
                FOREIGN KEY(`analysisId`) REFERENCES `SkinAnalysisEntry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())

            // create index on foreign key (if is on @Entity)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_LangerLineEntity_analysisId` ON `LangerLineEntity` (`analysisId`)")
        }
    }

    val MIGRATION_2_3 = object : Migration (2,3){
        override fun migrate(db: SupportSQLiteDatabase) {
            // no migration needed
        }
    }

    val MIGRATION_3_4 = object : Migration (3,4){
        override fun migrate(db: SupportSQLiteDatabase) {
            // no migration needed
        }
    }

    val MIGRATION_4_5 = object : Migration (4,5){
        override fun migrate(db: SupportSQLiteDatabase) {
            // no migration needed
        }
    }

    val MIGRATION_5_6 = object : Migration (5,6){
        override fun migrate(db: SupportSQLiteDatabase) {
            // no migration on scheme
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `targetRoute` TEXT DEFAULT NULL")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `insertedAtMs` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `skin_analyses` ADD COLUMN `syncFailed` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `skin_analyses` ADD COLUMN `userId` TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `skin_analyses` ADD COLUMN `lineCount` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `skin_analyses` ADD COLUMN `avgIntensity` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `skin_analyses` ADD COLUMN `tensionLevel` TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_11_12 = object : Migration (11,12){
        override fun migrate(db: SupportSQLiteDatabase) {

        }
    }


}