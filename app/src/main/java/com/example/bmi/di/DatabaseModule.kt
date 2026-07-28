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

@Module//@Module：标记这个类是“依赖提供者”的集合。
@InstallIn(SingletonComponent::class)
//告诉 Hilt，这些依赖的作用域是 Application 级别，App 启动时创建，App 销毁时释放。
object DatabaseModule {

    @Provides//告诉 Hilt：“你要 BmiDatabase 的时候，按我写的方法执行。”
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BmiDatabase {
        //拿 Application 的上下文，避免内存泄漏。
        return BmiDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideBmiDao(database: BmiDatabase): BmiDao {
        return database.bmiDao()
    }
}