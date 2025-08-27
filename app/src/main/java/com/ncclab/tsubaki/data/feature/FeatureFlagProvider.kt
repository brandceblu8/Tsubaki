package com.ncclab.tsubaki.data.feature

import com.ncclab.tsubaki.data.model.EngineType

interface FeatureFlagProvider {
    fun setActiveEngine(engine: EngineType)
    fun getActiveEngine(): EngineType
}