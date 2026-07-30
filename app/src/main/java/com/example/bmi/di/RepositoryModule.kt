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
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBmiRepository(impl: BmiRepositoryImpl): BmiRepository
}