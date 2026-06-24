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
}