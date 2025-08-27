package com.ncclab.tsubaki.data.scanner.strategy

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
import java.util.EnumMap

class ZXingScannerStrategy : ScannerStrategy {
    // 将字节数组声明为成员变量以复用，避免重复内存分配
    private var yuvBytes: ByteArray? = null

    private val reader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            // ✅ 修正：明确指定要解码的格式，提升效率
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
            // 确保 imageProxy 总是被关闭
            imageProxy.close()
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
            yPlane.rowStride,
            height,          // 这里使用 imageProxy 的 height
            0,
            0,
            width,           // 这里使用 imageProxy 的 width
            height,
            false
        )
    }
}