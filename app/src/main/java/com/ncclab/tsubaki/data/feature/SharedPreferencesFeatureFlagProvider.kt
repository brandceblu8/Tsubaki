package com.ncclab.tsubaki.data.feature

import android.content.Context
import com.ncclab.tsubaki.data.model.EngineType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesFeatureFlagProvider @Inject constructor( // <--- 在这里添加 @Inject
    @ApplicationContext context: Context
) : FeatureFlagProvider {
    private val prefs = context.getSharedPreferences("scanner_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_ENGINE = "active_engine"
    }

    override fun setActiveEngine(engine: EngineType) {
        prefs.edit().putString(KEY_ACTIVE_ENGINE, engine.name).apply()
    }

    override fun getActiveEngine(): EngineType {
        val engineName = prefs.getString(KEY_ACTIVE_ENGINE, EngineType.ML_KIT.name)
        return EngineType.valueOf(engineName?: EngineType.ML_KIT.name)
    }
}