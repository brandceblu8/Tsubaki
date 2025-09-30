package com.ncclab.tsubaki.data.scanner.strategy

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.ncclab.tsubaki.data.model.ScanResult

interface ScannerStrategy {
    fun analyze(imageProxy: ImageProxy, onResult: (List<ScanResult>) -> Unit)
    fun analyzeImage(bitmap: Bitmap, onResult: (List<ScanResult>) -> Unit)
    fun release()
}