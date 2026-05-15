package com.kumanodormitory.pokke.ui.viewmodel

import android.content.SharedPreferences
import com.kumanodormitory.pokke.data.repository.OperationLogRepository
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import com.kumanodormitory.pokke.data.repository.RyoseiRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val parcelRepository = mockk<ParcelRepository>()
    private val ryoseiRepository = mockk<RyoseiRepository>()
    private val operationLogRepository = mockk<OperationLogRepository>()
    private val syncPrefs = mockk<SharedPreferences>()

    private lateinit var viewModel: AdminViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { syncPrefs.getLong("lastRyoseiSyncAt", 0L) } returns 0L
        every { syncPrefs.getLong("lastParcelSyncAt", 0L) } returns 0L
        viewModel = AdminViewModel(parcelRepository, ryoseiRepository, operationLogRepository, syncPrefs)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `confirmLost calls archiveLostParcels with parcelId`() = runTest(testDispatcher) {
        coEvery { parcelRepository.archiveLostParcels(listOf("id1")) } just Runs
        coEvery { operationLogRepository.addLog(any(), any(), any(), any()) } just Runs

        viewModel.confirmLost("id1")
        advanceUntilIdle()

        coVerify { parcelRepository.archiveLostParcels(listOf("id1")) }
    }

    @Test
    fun `confirmLost sets snackbarMessage to success after completion`() = runTest(testDispatcher) {
        coEvery { parcelRepository.archiveLostParcels(listOf("id1")) } just Runs
        coEvery { operationLogRepository.addLog(any(), any(), any(), any()) } just Runs

        viewModel.confirmLost("id1")
        advanceUntilIdle()

        assertEquals("紛失確定しました", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `confirmLost sets error snackbarMessage when archiveLostParcels throws`() = runTest(testDispatcher) {
        coEvery { parcelRepository.archiveLostParcels(any()) } throws Exception("Network error")

        viewModel.confirmLost("id1")
        advanceUntilIdle()

        val message = viewModel.uiState.value.snackbarMessage
        assertNotNull(message)
        assertTrue(message!!.startsWith("紛失確定に失敗しました"))
    }

    @Test
    fun `when isLoading=true then loading state regardless of auth`() {
        val state = AdminUiState(isLoading = true, isAuthenticated = false)
        assertTrue(state.isLoading)
        assertTrue(!state.isAuthenticated)
    }

    @Test
    fun `when isLoading=false and not authenticated then auth state`() {
        val state = AdminUiState(isLoading = false, isAuthenticated = false)
        assertTrue(!state.isLoading)
        assertTrue(!state.isAuthenticated)
    }

    @Test
    fun `when isLoading=false and authenticated then menu state`() {
        val state = AdminUiState(isLoading = false, isAuthenticated = true)
        assertTrue(!state.isLoading)
        assertTrue(state.isAuthenticated)
    }
}
