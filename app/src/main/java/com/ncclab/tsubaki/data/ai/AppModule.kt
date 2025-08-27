package com.ncclab.tsubaki.data.ai

import com.ncclab.tsubaki.data.feature.FeatureFlagProvider
import com.ncclab.tsubaki.data.feature.SharedPreferencesFeatureFlagProvider
import com.ncclab.tsubaki.data.repository.ScanningRepository
import com.ncclab.tsubaki.data.repository.ScanningRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindScanningRepository(
        scanningRepositoryImpl: ScanningRepositoryImpl
    ): ScanningRepository

    @Binds
    @Singleton
    abstract fun bindFeatureFlagProvider(
        sharedPreferencesFeatureFlagProvider: SharedPreferencesFeatureFlagProvider
    ): FeatureFlagProvider
}