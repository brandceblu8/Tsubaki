package com.ncclab.tsubaki.data.repository

import com.ncclab.tsubaki.data.feature.FeatureFlagProvider
import com.ncclab.tsubaki.data.model.EngineType
import com.ncclab.tsubaki.data.model.ScanResult
import com.ncclab.tsubaki.data.scanner.factory.ScannerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanningRepositoryImplTest {

    private class FakeFeatureFlagProvider(
        private var engine: EngineType = EngineType.ML_KIT
    ) : FeatureFlagProvider {
        override fun setActiveEngine(engine: EngineType) {
            this.engine = engine
        }

        override fun getActiveEngine(): EngineType = engine
    }

    private fun newRepository(): ScanningRepositoryImpl {
        // The unit test exercises handleResultsForTest directly, which never reaches
        // the factory or strategy. A real ScannerFactory is fine because nothing in
        // this test path calls factory.create.
        return ScanningRepositoryImpl(ScannerFactory(), FakeFeatureFlagProvider())
    }

    private fun currentResults(repository: ScanningRepositoryImpl): List<ScanResult> {
        // scanResults is exposed as a Flow but is backed by a MutableStateFlow.
        // Reading the field directly lets us assert the latched value synchronously.
        val field = ScanningRepositoryImpl::class.java.getDeclaredField("_scanResults")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val state = field.get(repository) as MutableStateFlow<List<ScanResult>>
        return state.value
    }

    @Test
    fun `first non-empty detection latches and subsequent detections are dropped`() {
        val repository = newRepository()
        val first = listOf(ScanResult("first", "QR_CODE", 1L))
        val second = listOf(ScanResult("second", "QR_CODE", 2L))

        repository.handleResultsForTest(first)
        repository.handleResultsForTest(second)

        assertEquals(first, currentResults(repository))
    }

    @Test
    fun `acknowledgeResult clears the latch and the emitted results`() {
        val repository = newRepository()
        val first = listOf(ScanResult("first", "QR_CODE", 1L))
        val second = listOf(ScanResult("second", "QR_CODE", 2L))

        repository.handleResultsForTest(first)
        assertEquals(first, currentResults(repository))

        repository.acknowledgeResult()
        assertTrue(currentResults(repository).isEmpty())

        repository.handleResultsForTest(second)
        assertEquals(second, currentResults(repository))
    }

    @Test
    fun `empty results never latch and do not overwrite existing results`() {
        val repository = newRepository()
        val first = listOf(ScanResult("first", "QR_CODE", 1L))

        repository.handleResultsForTest(emptyList())
        assertTrue(currentResults(repository).isEmpty())

        repository.handleResultsForTest(first)
        assertEquals(first, currentResults(repository))

        // Another empty pass after latching must not clear the value.
        repository.handleResultsForTest(emptyList())
        assertEquals(first, currentResults(repository))
    }

    @Test
    fun `resetScanner discards in-flight callbacks from the released strategy`() {
        val repository = newRepository()
        val stale = listOf(ScanResult("stale-from-old-engine", "QR_CODE", 1L))
        val fresh = listOf(ScanResult("fresh-from-new-engine", "QR_CODE", 2L))

        // Snapshot the generation that an in-flight analyze callback would
        // have captured before the user switched engines.
        val dispatchGeneration = repository.currentGeneration

        // The user opens the engine dialog and confirms a switch. resetScanner
        // releases the old strategy, clears the latch, and bumps the generation.
        repository.resetScanner()

        // The old strategy's success listener now lands. Without the
        // generation check this would emit a result decoded by the engine
        // the user just switched away from.
        repository.handleResultsForTest(stale, dispatchGeneration = dispatchGeneration)

        assertTrue(
            "Stale callback from released strategy must be ignored",
            currentResults(repository).isEmpty(),
        )

        // The next detection (using the freshly-selected engine) is dispatched
        // against the new generation and is published normally.
        repository.handleResultsForTest(fresh)
        assertEquals(fresh, currentResults(repository))
    }
}
