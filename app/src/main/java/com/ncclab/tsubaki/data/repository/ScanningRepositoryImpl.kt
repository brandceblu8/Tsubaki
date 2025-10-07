package com.ncclab.tsubaki.data.repository

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import com.ncclab.tsubaki.data.feature.FeatureFlagProvider
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

    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    override val scanResults: Flow<List<ScanResult>> = _scanResults.asStateFlow()

    override val analyzer: ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { imageProxy ->
        if (activeScanner == null) {
            activeScanner = scannerFactory.create(featureFlagProvider.getActiveEngine())
        }

        activeScanner?.analyze(imageProxy) { results ->
            if (results.isNotEmpty()) {
                _scanResults.value = results
            }
        }
    }

    override fun scanFromBitmap(bitmap: Bitmap) {
        if (activeScanner == null) {
            activeScanner = scannerFactory.create(featureFlagProvider.getActiveEngine())
        }

        activeScanner?.analyzeImage(bitmap) { results ->
            if (results.isNotEmpty()) {
                _scanResults.value = results
            }
        }
    }

    override fun resetScanner() {
        activeScanner?.release()
        activeScanner = null
    }
}