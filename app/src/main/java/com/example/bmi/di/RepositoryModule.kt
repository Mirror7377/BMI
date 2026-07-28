package com.example.bmi.di

import com.example.bmi.data.repository.BmiRepository
import com.example.bmi.data.repository.BmiRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
//我这个 Module 里提供的所有依赖，都要存放在‘全局 Application 级’的容器里
//SingletonComponent 是最顶层的根容器。它在 App 启动时创建，在 App 进程被杀死时销毁
abstract class RepositoryModule {

    @Binds//当有人请求 BmiRepository 接口时，把 BmiRepositoryImpl 实现类给它
    @Singleton
    abstract fun bindBmiRepository(impl: BmiRepositoryImpl): BmiRepository
}