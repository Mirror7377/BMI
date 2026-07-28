package com.example.bmi.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BmiRecord::class], version = 4, exportSchema = false)
abstract class BmiDatabase : RoomDatabase() {
    abstract fun bmiDao(): BmiDao

    companion object {

        @Volatile
        private var INSTANCE: BmiDatabase? = null

        fun getInstance(context: Context): BmiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,//  上下文
                    BmiDatabase::class.java,//  数据库抽象类
                    "bmi_database"//  数据库文件名
                ).fallbackToDestructiveMigration()//如果没有提供具体的迁移逻辑，Room 就直接把旧数据全部删除，重新创建一张空表。
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}