package com.thikeiw.lolinfo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ChampionEntity::class, SkinEntity::class], version = 3, exportSchema = false)
@TypeConverters(StringListConverter::class)
abstract class CatalogDatabase : RoomDatabase() {
  abstract fun catalogDao(): CatalogDao
}
