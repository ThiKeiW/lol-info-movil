package com.thikeiw.lolinfo.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.thikeiw.lolinfo.data.local.CatalogDao
import com.thikeiw.lolinfo.data.local.CatalogDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Fase 3: `skin_flags` pasa de PK simple (skinId) a PK compuesta (uid, skinId) y gana columnas
 * para el reloj lógico. SQLite no permite cambiar la PK con ALTER, así que se recrea la tabla.
 * Las marcas existentes se guardan como `uid = "legacy"` y se reclaman al iniciar sesión.
 */
private val MIGRATION_4_5 =
  object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS skin_flags_new (
          uid TEXT NOT NULL,
          skinId INTEGER NOT NULL,
          isFavorite INTEGER NOT NULL,
          isOwned INTEGER NOT NULL,
          revision INTEGER NOT NULL,
          updatedBy TEXT NOT NULL,
          pendingSync INTEGER NOT NULL,
          PRIMARY KEY(uid, skinId)
        )
        """.trimIndent()
      )
      db.execSQL(
        "INSERT INTO skin_flags_new (uid, skinId, isFavorite, isOwned, revision, updatedBy, pendingSync) " +
          "SELECT 'legacy', skinId, isFavorite, isOwned, 0, '', 1 FROM skin_flags"
      )
      db.execSQL("DROP TABLE skin_flags")
      db.execSQL("ALTER TABLE skin_flags_new RENAME TO skin_flags")
      db.execSQL("CREATE INDEX IF NOT EXISTS index_skin_flags_uid ON skin_flags (uid)")
    }
  }

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideCatalogDatabase(@ApplicationContext context: Context): CatalogDatabase =
    Room.databaseBuilder(context, CatalogDatabase::class.java, "lolskin.db")
      .addMigrations(MIGRATION_4_5)
      .fallbackToDestructiveMigration(true)
      .build()

  @Provides
  @Singleton fun provideCatalogDao(database: CatalogDatabase): CatalogDao = database.catalogDao()
}
