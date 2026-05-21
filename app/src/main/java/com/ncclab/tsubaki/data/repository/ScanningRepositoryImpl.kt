package com.ncclab.tsubaki.data.repository

import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageAnalysis
import com.ncclab.tsubaki.data.feature.FeatureFlagProvider
import com.ncclab.tsubaki.data.model.EngineType
import com.ncclab.tsubaki.data.model.ScanResult
import com.ncclab.tsubaki.data.scanner.factory.ScannerFactory
import com.ncclab.tsubaki.data.scanner.strategy.ScannerStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanningRepositoryImpl @Inject constructor(
    private val scannerFactory: ScannerFactory,
    private val featureFlagProvider: FeatureFlagProvider
) : ScanningRepository {

    private var activeScanner: ScannerStrategy? = null
    private var activeEngine: EngineType? = null

    @Volatile
    private var detectionLatched = false

    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    override val scanResults: Flow<List<ScanResult>> = _scanResults.asStateFlow()

    override val analyzer: ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { imageProxy ->
        if (detectionLatched) {
            imageProxy.close()
            return@Analyzer
        }

        ensureActiveScanner().analyze(imageProxy) { results ->
            handleResults(results)
        }
    }

    override fun scanFromBitmap(bitmap: Bitmap) {
        if (detectionLatched) return

        ensureActiveScanner().analyzeImage(bitmap) { results ->
            handleResults(results)
        }
    }

    override fun resetScanner() {
        activeScanner?.release()
        activeScanner = null
        activeEngine = null
        detectionLatched = false
    }

    override fun acknowledgeResult() {
        detectionLatched = false
        _scanResults.value = emptyList()
    }

    private fun ensureActiveScanner(): ScannerStrategy {
        val scanner = activeScanner
        if (scanner != null) return scanner

        val engine = featureFlagProvider.getActiveEngine()
        val newScanner = scannerFactory.create(engine)
        activeScanner = newScanner
        activeEngine = engine
        return newScanner
    }

    private fun handleResults(results: List<ScanResult>) {
        if (detectionLatched) return
        if (results.isEmpty()) return
        detectionLatched = true
        _scanResults.value = results
    }

    @VisibleForTesting
    internal fun handleResultsForTest(results: List<ScanResult>) = handleResults(results)
}
