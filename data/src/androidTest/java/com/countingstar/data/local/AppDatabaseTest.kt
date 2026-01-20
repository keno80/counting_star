package com.countingstar.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase
    private lateinit var greetingDao: GreetingDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        greetingDao = database.greetingDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsert_and_observeGreeting() =
        runBlocking {
            greetingDao.upsert(GreetingEntity(text = "Hello"))
            val result = greetingDao.observeGreeting().first()
            assertEquals("Hello", result?.text)
        }
}
