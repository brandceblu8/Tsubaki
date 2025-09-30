package com.ncclab.tsubaki.data.scanner.strategy

import android.graphics.Bitmap
import android.util.Log
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
            val luminanceSource = imageProxy.toLuminanceSource() ?: return // 如果转换失败则提前返回
            val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))
            val result = reader.decodeWithState(binaryBitmap)

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

    override fun analyzeImage(bitmap: Bitmap, onResult: (List<ScanResult>) -> Unit) {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val luminanceSource = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))
            val result = reader.decodeWithState(binaryBitmap)

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
        reader.reset()
    }

    private fun ImageProxy.toLuminanceSource(): PlanarYUVLuminanceSource? {
        // ✅ 修正：从 planes 数组中获取第一个平面
        val yPlane = planes.firstOrNull() ?: return null
        val yBuffer = yPlane.buffer
        val ySize = yBuffer.remaining()

        // ✅ 优化：复用 ByteArray 以减少垃圾回收
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