package com.bhikan.airtap.di

import android.content.Context
import com.bhikan.airtap.data.repository.FileRepository
import com.bhikan.airtap.data.repository.FileRepositoryImpl
import com.bhikan.airtap.data.repository.SmsRepository
import com.bhikan.airtap.data.repository.UserRepository
import com.bhikan.airtap.server.auth.AuthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFileRepository(
        @ApplicationContext context: Context
    ): FileRepository = FileRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideAuthManager(
        @ApplicationContext context: Context
    ): AuthManager = AuthManager(context)

    @Provides
    @Singleton
    fun provideSmsRepository(
        @ApplicationContext context: Context
    ): SmsRepository = SmsRepository(context)

    @Provides
    @Singleton
    fun provideUserRepository(
        @ApplicationContext context: Context
    ): UserRepository = UserRepository(context)
}
