package com.thikeiw.lolinfo.di

import com.thikeiw.lolinfo.ui.championaudio.AndroidAudioPlayer
import com.thikeiw.lolinfo.ui.championaudio.AudioPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
  @Binds @Singleton abstract fun bindAudioPlayer(impl: AndroidAudioPlayer): AudioPlayer
}
