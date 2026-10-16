package com.tenderready.png.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * Schema version 1. For future changes: bump version, add a Migration to MIGRATIONS.
 * Destructive migration is deliberately NOT enabled, so an app update can never wipe user data.
 */
@Database(
    entities = [
        BusinessProfile::class,
        CapabilityStatement::class,
        Quotation::class,
        QuotationItem::class,
        TenderOpportunity::class,
        TenderRequirement::class,
        ReadinessItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun capabilityDao(): CapabilityDao
    abstract fun quotationDao(): QuotationDao
    abstract fun tenderDao(): TenderDao
    abstract fun readinessDao(): ReadinessDao

    companion object {
        val MIGRATIONS: Array<Migration> = arrayOf()

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tenderready.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
