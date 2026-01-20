package com.countingstar.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.countingstar.data.GreetingRepositoryImpl
import com.countingstar.data.local.AccountDao
import com.countingstar.data.local.AppDatabase
import com.countingstar.data.local.GreetingDao
import com.countingstar.data.local.LedgerDao
import com.countingstar.domain.GreetingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                "counting_star.db",
            ).build()

    @Provides
    fun provideGreetingDao(database: AppDatabase): GreetingDao = database.greetingDao()

    @Provides
    fun provideLedgerDao(database: AppDatabase): LedgerDao = database.ledgerDao()

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings") },
        )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGreetingRepository(impl: GreetingRepositoryImpl): GreetingRepository
}
