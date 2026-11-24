package com.thikeiw.lolinfo.di

import com.thikeiw.lolinfo.data.CatalogRepository
import com.thikeiw.lolinfo.data.DefaultCatalogRepository
import com.thikeiw.lolinfo.data.auth.AuthRepository
import com.thikeiw.lolinfo.data.auth.FirebaseAuthRepository
import com.thikeiw.lolinfo.data.sync.FirestoreSkinFlagSyncRepository
import com.thikeiw.lolinfo.data.sync.SkinFlagSyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
  @Binds @Singleton abstract fun bindCatalogRepository(impl: DefaultCatalogRepository): CatalogRepository

  @Binds @Singleton abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

  @Binds
  @Singleton
  abstract fun bindSkinFlagSyncRepository(impl: FirestoreSkinFlagSyncRepository): SkinFlagSyncRepository
}
