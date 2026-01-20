package com.countingstar.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.countingstar.data.local.GreetingDao
import com.countingstar.data.local.GreetingEntity
import com.countingstar.domain.GreetingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GreetingRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        private val greetingDao: GreetingDao,
    ) : GreetingRepository {
        private val greetingKey = stringPreferencesKey("greeting_text")

        override fun greetingFlow(): Flow<String> {
            val dataStoreFlow =
                dataStore.data.map { preferences ->
                    preferences[greetingKey]
                }
            val roomFlow = greetingDao.observeGreeting().map { it?.text }
            return combine(dataStoreFlow, roomFlow) { dataStoreValue, roomValue ->
                dataStoreValue ?: roomValue ?: "Counting Star"
            }.distinctUntilChanged()
        }

        override suspend fun setGreeting(text: String) {
            dataStore.edit { preferences ->
                preferences[greetingKey] = text
            }
            greetingDao.upsert(GreetingEntity(text = text))
        }
    }
