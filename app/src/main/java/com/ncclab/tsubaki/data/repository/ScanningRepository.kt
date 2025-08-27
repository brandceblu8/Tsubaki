package com.ncclab.tsubaki.data.repository

import androidx.camera.core.ImageAnalysis
import kotlinx.coroutines.flow.Flow
import com.ncclab.tsubaki.data.model.ScanResult

interface ScanningRepository {
    val scanResults: Flow<List<ScanResult>>
    // ✅ 修改: 将 getAnalyzer() 函数改为 val analyzer 属性
    val analyzer: ImageAnalysis.Analyzer
    fun resetScanner()
}