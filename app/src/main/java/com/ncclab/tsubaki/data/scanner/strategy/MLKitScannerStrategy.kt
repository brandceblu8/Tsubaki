package com.ncclab.tsubaki.data.scanner.strategy

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.ncclab.tsubaki.data.model.ScanResult

class MLKitScannerStrategy : ScannerStrategy {
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_DATA_MATRIX
        )
        .build()
    private val scanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy, onResult: (List<ScanResult>) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // ✅ 使用最终的、正确的处理逻辑
                    val scanResults = barcodes.mapNotNull { it.toScanResult() }
                    onResult(scanResults)
                }
                .addOnFailureListener {
                    onResult(emptyList())
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
            onResult(emptyList())
        }
    }

    override fun release() {
        scanner.close()
    }

    private fun Barcode.toScanResult(): ScanResult? {
        val value = rawValue ?: displayValue ?: return null // 如果两者都为null，则返回null
        return ScanResult(
            rawValue = value,
            format = barcodeFormatToString(format),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun barcodeFormatToString(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            else -> "UNKNOWN ($format)"
        }
    }
}