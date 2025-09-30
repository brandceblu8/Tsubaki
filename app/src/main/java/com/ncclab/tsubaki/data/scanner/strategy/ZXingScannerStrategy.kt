package com.ncclab.tsubaki.data.scanner.strategy

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.ncclab.tsubaki.data.model.ScanResult
import com.google.zxing.RGBLuminanceSource
import java.util.EnumMap

class ZXingScannerStrategy : ScannerStrategy {
    private var yuvBytes: ByteArray? = null

    private val reader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.POSSIBLE_FORMATS, listOf(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.CODE_128,
                BarcodeFormat.EAN_13,
                BarcodeFormat.DATA_MATRIX
            ))
        }
        setHints(hints)
    }

    override fun analyze(imageProxy: ImageProxy, onResult: (List<ScanResult>) -> Unit) {
        try {
            Log.d("ZXingScanner", "Starting analysis, image size: ${imageProxy.width}x${imageProxy.height}")

            val luminanceSource = imageProxy.toLuminanceSource() ?: return
            val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))
            val result = reader.decodeWithState(binaryBitmap)

            Log.d("ZXingScanner", "Successfully decoded: ${result.text}")

            val scanResult = ScanResult(
                rawValue = result.text,
                format = result.barcodeFormat.toString(),
                timestamp = System.currentTimeMillis()
            )
            onResult(listOf(scanResult))
        } catch (e: NotFoundException) {
            // 未找到条码，这是正常情况，无需处理
        } catch (e: Exception) {
            Log.e("ZXingScanner", "ZXing decoding failed", e)
        } finally {
            imageProxy.close()
        }
    }

    // 完全替换 ZXingScannerStrategy.kt 中的 analyzeImage 方法
    @RequiresApi(Build.VERSION_CODES.O)
    override fun analyzeImage(bitmap: Bitmap, onResult: (List<ScanResult>) -> Unit) {
        try {
            Log.d("ZXingScanner", "Analyzing bitmap, config: ${bitmap.config}, size: ${bitmap.width}x${bitmap.height}")

            // 创建一个新的软件 Bitmap
            val softwareBitmap = when {
                bitmap.config == Bitmap.Config.HARDWARE -> {
                    Log.d("ZXingScanner", "Converting HARDWARE bitmap to software bitmap")
                    // 创建一个新的软件配置的 Bitmap
                    val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(newBitmap)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    newBitmap
                }
                bitmap.config == null -> {
                    Log.d("ZXingScanner", "Bitmap config is null, creating ARGB_8888 copy")
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                }
                else -> {
                    Log.d("ZXingScanner", "Using original bitmap with config: ${bitmap.config}")
                    bitmap
                }
            }

            val width = softwareBitmap.width
            val height = softwareBitmap.height
            val pixels = IntArray(width * height)

            Log.d("ZXingScanner", "Getting pixels from bitmap")
            softwareBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            Log.d("ZXingScanner", "Creating luminance source")
            val luminanceSource = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))

            Log.d("ZXingScanner", "Attempting to decode")
            val result = reader.decodeWithState(binaryBitmap)

            Log.d("ZXingScanner", "Successfully decoded bitmap: ${result.text}")

            val scanResult = ScanResult(
                rawValue = result.text,
                format = result.barcodeFormat.toString(),
                timestamp = System.currentTimeMillis()
            )
            onResult(listOf(scanResult))

            // 如果我们创建了新的 Bitmap，需要回收它
            if (softwareBitmap != bitmap) {
                softwareBitmap.recycle()
            }

        } catch (e: NotFoundException) {
            Log.d("ZXingScanner", "No barcode found in bitmap")
            onResult(emptyList())
        } catch (e: Exception) {
            Log.e("ZXingScanner", "ZXing bitmap decoding failed", e)
            onResult(emptyList())
        }
    }

    override fun release() {
        reader.reset()
    }

    private fun ImageProxy.toLuminanceSource(): PlanarYUVLuminanceSource? {
        val yPlane = planes.firstOrNull() ?: return null
        val yBuffer = yPlane.buffer
        val ySize = yBuffer.remaining()

        val bytes = yuvBytes.let {
            if (it == null || it.size < ySize) {
                ByteArray(ySize).also { newBytes -> yuvBytes = newBytes }
            } else {
                it
            }
        }
        yBuffer.get(bytes, 0, ySize)

        return PlanarYUVLuminanceSource(
            bytes,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
    }
}