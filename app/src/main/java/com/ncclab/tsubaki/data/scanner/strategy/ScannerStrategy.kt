package com.ncclab.tsubaki.data.scanner.strategy

import androidx.camera.core.ImageProxy
import com.ncclab.tsubaki.data.model.ScanResult

interface ScannerStrategy {
    fun analyze(imageProxy: ImageProxy, onResult: (List<ScanResult>) -> Unit)
    fun release()
}