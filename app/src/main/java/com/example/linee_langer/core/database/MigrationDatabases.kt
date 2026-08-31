package com.example.linee_langer.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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


}