package com.ncclab.tsubaki.data.scanner.strategy

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.ncclab.tsubaki.data.model.ScanResult

class MLKitScannerStrategyBak : ScannerStrategy {
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
            Log.d("TsubakiScan", "MLKit: Processing image with rotation ${imageProxy.imageInfo.rotationDegrees} and size ${mediaImage.width}x${mediaImage.height}")

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // ✅ 修正1：把日志移到循环外面
                    Log.d("TsubakiScan", "MLKit: Success. Found ${barcodes.size} barcodes.")

                    // ✅ 修正2：即使barcodes不为空，也要检查map转换后的结果
                    val scanResults = barcodes.mapNotNull { it.toScanResult() }

                    // 无论转换后结果是否为空，都调用 onResult
                    // 如果 barcodes 为空，scanResults 自然也为空，这个调用是安全的
                    onResult(scanResults)
                }
                .addOnFailureListener { e ->
                    Log.e("TsubakiScan", "MLKit: Barcode scanning failed", e)
                    // ✅ 修正3：在失败时也调用 onResult，并传递空列表
                    onResult(emptyList())
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    Log.d("TsubakiScan", "MLKit: ImageProxy closed.")
                }
        } else {
            imageProxy.close()
            // 在 mediaImage 为空时也通知上层
            onResult(emptyList())
        }
    }

    override fun release() {
        scanner.close()
    }

    private fun Barcode.toScanResult(): ScanResult? {
        return rawValue?.let {
            ScanResult(
                rawValue = it,
                format = barcodeFormatToString(format),
                timestamp = System.currentTimeMillis()
            )
        }
    }

    private fun barcodeFormatToString(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            // 添加更多你可能需要的格式
            else -> "UNKNOWN ($format)"
        }
    }
}