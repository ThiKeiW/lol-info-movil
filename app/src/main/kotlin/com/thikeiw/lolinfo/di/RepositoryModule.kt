package com.thikeiw.lolinfo.di

import com.thikeiw.lolinfo.data.CatalogRepository
import com.thikeiw.lolinfo.data.DefaultCatalogRepository
import com.thikeiw.lolinfo.data.audio.ChampionAudioChampionSource
import com.thikeiw.lolinfo.data.audio.ChampionAudioLocaleSource
import com.thikeiw.lolinfo.data.audio.ChampionAudioRepository
import com.thikeiw.lolinfo.data.audio.DefaultChampionAudioChampionSource
import com.thikeiw.lolinfo.data.audio.DefaultChampionAudioRepository
import com.thikeiw.lolinfo.data.audio.PreferencesChampionAudioLocaleSource
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

  @Binds
  @Singleton
  abstract fun bindChampionAudioRepository(impl: DefaultChampionAudioRepository): ChampionAudioRepository

  @Binds
  @Singleton
  abstract fun bindChampionAudioChampionSource(
    impl: DefaultChampionAudioChampionSource,
  ): ChampionAudioChampionSource

  @Binds
  @Singleton
  abstract fun bindChampionAudioLocaleSource(
    impl: PreferencesChampionAudioLocaleSource,
  ): ChampionAudioLocaleSource
}
