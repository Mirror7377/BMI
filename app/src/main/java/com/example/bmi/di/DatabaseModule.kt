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
//告诉 Hilt，这些依赖的作用域是 Application 级别，App 启动时创建，App 销毁时释放。
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