package com.thikeiw.lolinfo.di

import android.content.Context
import androidx.room.Room
import com.thikeiw.lolinfo.data.local.CatalogDao
import com.thikeiw.lolinfo.data.local.CatalogDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideCatalogDatabase(@ApplicationContext context: Context): CatalogDatabase =
    Room.databaseBuilder(context, CatalogDatabase::class.java, "lolskin.db")
      .fallbackToDestructiveMigration(true)
      .build()

  @Provides
  @Singleton fun provideCatalogDao(database: CatalogDatabase): CatalogDao = database.catalogDao()
}
