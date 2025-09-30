package com.ncclab.tsubaki.data.scanner.strategy

import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.Reader
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
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
    private val reader: Reader = MultiFormatReader()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy, onResult: (List<ScanResult>) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            Log.d("TsubakiScan", "MLKit: Processing image with rotation ${imageProxy.imageInfo.rotationDegrees} and size ${mediaImage.width}x${mediaImage.height}")

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    Log.d("TsubakiScan", "MLKit: Success. Found ${barcodes.size} barcodes.")
                    val scanResults = barcodes.mapNotNull { it.toScanResult() }
                    onResult(scanResults)
                }
                .addOnFailureListener { e ->
                    Log.e("TsubakiScan", "MLKit: Barcode scanning failed", e)
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

    override fun analyzeImage(bitmap: Bitmap, onResult: (List<ScanResult>) -> Unit) {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val luminanceSource = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))
            val result = reader.decode(binaryBitmap)

            val scanResult = ScanResult(
                rawValue = result.text,
                format = result.barcodeFormat.toString(),
                timestamp = System.currentTimeMillis()
            )
            onResult(listOf(scanResult))
        } catch (e: NotFoundException) {
            onResult(emptyList())
        } catch (e: Exception) {
            Log.e("ZXingScanner", "ZXing bitmap decoding failed", e)
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