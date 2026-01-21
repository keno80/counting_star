package com.countingstar.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.countingstar.data.AccountRepositoryImpl
import com.countingstar.data.CategoryRepositoryImpl
import com.countingstar.data.GreetingRepositoryImpl
import com.countingstar.data.LedgerRepositoryImpl
import com.countingstar.data.MerchantRepositoryImpl
import com.countingstar.data.PreferenceRepositoryImpl
import com.countingstar.data.TagRepositoryImpl
import com.countingstar.data.TransactionRepositoryImpl
import com.countingstar.data.local.AccountDao
import com.countingstar.data.local.AppDatabase
import com.countingstar.data.local.CategoryDao
import com.countingstar.data.local.GreetingDao
import com.countingstar.data.local.LedgerDao
import com.countingstar.data.local.MerchantDao
import com.countingstar.data.local.TagDao
import com.countingstar.data.local.TransactionDao
import com.countingstar.data.local.TransactionSplitDao
import com.countingstar.domain.AccountRepository
import com.countingstar.domain.CategoryRepository
import com.countingstar.domain.GreetingRepository
import com.countingstar.domain.LedgerRepository
import com.countingstar.domain.MerchantRepository
import com.countingstar.domain.PreferenceRepository
import com.countingstar.domain.TagRepository
import com.countingstar.domain.TransactionRepository
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
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideMerchantDao(database: AppDatabase): MerchantDao = database.merchantDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideTransactionSplitDao(database: AppDatabase): TransactionSplitDao = database.transactionSplitDao()

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

    @Binds
    @Singleton
    abstract fun bindPreferenceRepository(impl: PreferenceRepositoryImpl): PreferenceRepository

    @Binds
    @Singleton
    abstract fun bindLedgerRepository(impl: LedgerRepositoryImpl): LedgerRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    @Singleton
    abstract fun bindMerchantRepository(impl: MerchantRepositoryImpl): MerchantRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
}
