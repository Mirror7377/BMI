package com.example.bmi.di


import android.content.Context
import com.example.bmi.data.database.BmiDao
import com.example.bmi.data.database.BmiDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BmiDatabase {
        return BmiDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideBmiDao(database: BmiDatabase): BmiDao {
        return database.bmiDao()
    }
}