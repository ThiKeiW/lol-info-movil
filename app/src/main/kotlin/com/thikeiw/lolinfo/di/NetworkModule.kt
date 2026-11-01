package com.thikeiw.lolinfo.di

import com.thikeiw.lolinfo.BuildConfig
import com.thikeiw.lolinfo.data.remote.CommunityDragonService
import com.thikeiw.lolinfo.data.remote.DataDragonService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  @Provides
  @Singleton
  fun provideJson(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
  }

  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient {
    val logging = HttpLoggingInterceptor().apply {
      level =
        if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
        else HttpLoggingInterceptor.Level.NONE
    }
    return OkHttpClient.Builder()
      .addInterceptor { chain ->
        chain.proceed(
          chain.request().newBuilder()
            .header("User-Agent", "LoLSkinScope/${BuildConfig.VERSION_NAME} (Android)")
            .build()
        )
      }
      .addInterceptor(logging)
      .build()
  }

  @Provides
  @Singleton
  fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
    Retrofit.Builder()
      .baseUrl(BuildConfig.DD_URL)
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()

  @Provides
  @Singleton
  fun provideDataDragonService(retrofit: Retrofit): DataDragonService =
    retrofit.create(DataDragonService::class.java)

  @Provides
  @Singleton
  fun provideCommunityDragonService(client: OkHttpClient, json: Json): CommunityDragonService =
    Retrofit.Builder()
      .baseUrl(BuildConfig.CDRAGON_URL)
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(CommunityDragonService::class.java)
}
