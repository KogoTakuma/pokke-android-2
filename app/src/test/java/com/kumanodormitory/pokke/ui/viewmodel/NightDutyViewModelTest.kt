package com.kumanodormitory.pokke.ui.viewmodel

import android.content.SharedPreferences
import com.kumanodormitory.pokke.data.local.entity.OperationLogEntity
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.repository.DutyPersonRepository
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NightDutyViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val parcelRepository = mockk<ParcelRepository>(relaxed = true)
    private val dutyPersonRepository = mockk<DutyPersonRepository>(relaxed = true)
    private lateinit var viewModel: NightDutyViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { dutyPersonRepository.getCurrentDutyPerson() } returns flowOf(null)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun makeParcel(
        id: String,
        isLost: Boolean = false,
        lastConfirmedAt: Long? = null,
        lostConfirmedAt: Long? = null,
    ): ParcelEntity = ParcelEntity(
        id = id,
        createdAt = 1000L,
        updatedAt = 1000L,
        ryoseiId = "r-$id",
        ownerBlock = "A1",
        ownerRoomName = "A101",
        ownerName = "テスト",
        parcelType = "NORMAL",
        registeredByName = "admin",
        isLost = isLost,
        lastConfirmedAt = lastConfirmedAt,
        lostConfirmedAt = lostConfirmedAt
    )

    private fun createViewModel(parcels: List<ParcelEntity>) {
        coEvery { parcelRepository.getRegisteredParcels() } returns flowOf(parcels)
        viewModel = NightDutyViewModel(parcelRepository, dutyPersonRepository)
    }

    private fun createViewModel(parcelsFlow: MutableStateFlow<List<ParcelEntity>>) {
        coEvery { parcelRepository.getRegisteredParcels() } returns parcelsFlow.asStateFlow()
        viewModel = NightDutyViewModel(parcelRepository, dutyPersonRepository)
    }

    @Test
    fun `lostIds は初期ロード時に DB の isLost=true 集合で埋まる`() = runTest(testDispatcher) {
        val lost = makeParcel("p1", isLost = true)
        val notLost = makeParcel("p2", isLost = false)
        createViewModel(listOf(lost, notLost))
        advanceUntilIdle()

        assertEquals(setOf("p1"), viewModel.uiState.value.lostIds)
    }

    @Test
    fun `現存荷物の completeNightDuty は lastConfirmedAt のみ更新し isLost を false にする`() = runTest(testDispatcher) {
        val previouslyLost = makeParcel("p1", isLost = true, lostConfirmedAt = 100L)
        createViewModel(listOf(previouslyLost))
        advanceUntilIdle()

        viewModel.toggleLost("p1")
        viewModel.advanceToPhase2()
        viewModel.toggleCheck("p1")

        val confirmedSlot = slot<List<ParcelEntity>>()
        val lostUpdatesSlot = slot<List<ParcelEntity>>()
        val lostLogsSlot = slot<List<OperationLogEntity>>()
        coEvery {
            parcelRepository.completeNightDutyAtomic(
                capture(confirmedSlot), capture(lostUpdatesSlot),
                capture(lostLogsSlot), any()
            )
        } returns Unit

        viewModel.completeNightDuty(prefs = null, onComplete = {})
        advanceUntilIdle()

        assertEquals(1, confirmedSlot.captured.size)
        val updated = confirmedSlot.captured.first()
        assertFalse("isLost should be false after un-mark", updated.isLost)
        assertTrue("lastConfirmedAt should be touched", updated.lastConfirmedAt != null && updated.lastConfirmedAt!! > 100L)
        assertEquals("lostConfirmedAt should remain original value", 100L, updated.lostConfirmedAt)
        assertTrue(lostUpdatesSlot.captured.isEmpty())
    }

    @Test
    fun `紛失荷物の completeNightDuty は lostConfirmedAt のみ更新し lastConfirmedAt を維持`() = runTest(testDispatcher) {
        val parcel = makeParcel("p1", isLost = false, lastConfirmedAt = 200L)
        createViewModel(listOf(parcel))
        advanceUntilIdle()

        viewModel.toggleLost("p1")
        viewModel.advanceToPhase2()
        viewModel.toggleCheck("p1")

        val lostUpdatesSlot = slot<List<ParcelEntity>>()
        coEvery {
            parcelRepository.completeNightDutyAtomic(any(), capture(lostUpdatesSlot), any(), any())
        } returns Unit

        viewModel.completeNightDuty(prefs = null, onComplete = {})
        advanceUntilIdle()

        assertEquals(1, lostUpdatesSlot.captured.size)
        val updated = lostUpdatesSlot.captured.first()
        assertTrue("isLost should be true", updated.isLost)
        assertTrue("lostConfirmedAt should be set", updated.lostConfirmedAt != null && updated.lostConfirmedAt!! > 0L)
        assertEquals("lastConfirmedAt should remain original value", 200L, updated.lastConfirmedAt)
    }

    @Test
    fun `MARK_LOST ログは既存 isLost=true 荷物には作成されず 真の遷移のみ`() = runTest(testDispatcher) {
        val alreadyLost = makeParcel("p1", isLost = true)
        val newlyLost = makeParcel("p2", isLost = false)
        createViewModel(listOf(alreadyLost, newlyLost))
        advanceUntilIdle()

        viewModel.toggleLost("p2")
        viewModel.toggleCheck("p1")
        viewModel.advanceToPhase2()
        viewModel.toggleCheck("p1")
        viewModel.toggleCheck("p2")

        val lostLogsSlot = slot<List<OperationLogEntity>>()
        coEvery {
            parcelRepository.completeNightDutyAtomic(any(), any(), capture(lostLogsSlot), any())
        } returns Unit

        viewModel.completeNightDuty(prefs = null, onComplete = {})
        advanceUntilIdle()

        assertEquals(1, lostLogsSlot.captured.size)
        assertEquals("p2", lostLogsSlot.captured.first().parcelId)
        assertEquals("MARK_LOST", lostLogsSlot.captured.first().operationType)
    }

    @Test
    fun `引き渡しで Flow が parcel を除外すると残骸 ID が除去される`() = runTest(testDispatcher) {
        val p1 = makeParcel("p1", isLost = false)
        val p2 = makeParcel("p2", isLost = false)
        val flow = MutableStateFlow(listOf(p1, p2))
        createViewModel(flow)
        advanceUntilIdle()

        viewModel.toggleCheck("p1")
        viewModel.toggleCheck("p2")
        assertEquals(setOf("p1", "p2"), viewModel.uiState.value.checkedIdsPhase1)
        assertTrue(viewModel.uiState.value.allCheckedPhase1)

        flow.value = listOf(p2)
        advanceUntilIdle()

        assertEquals(setOf("p2"), viewModel.uiState.value.checkedIdsPhase1)
        assertTrue(
            "allCheckedPhase1 should recompute true after residual cleanup",
            viewModel.uiState.value.allCheckedPhase1
        )
    }

    @Test
    fun `中断データは 5 時間未満なら fresh 5 時間以上なら stale`() {
        val prefs = mockk<SharedPreferences>(relaxed = true)
        createViewModel(emptyList<ParcelEntity>())

        val freshSavedAt = System.currentTimeMillis() - 60_000L
        val fresh = """{"phase":1,"checkedIdsPhase1":[],"checkedIdsPhase2":[],"lostIds":[],"savedAt":$freshSavedAt}"""
        every { prefs.getString("night_duty_suspended", null) } returns fresh
        assertFalse(viewModel.isSuspendedDataStale(prefs))

        val staleSavedAt = System.currentTimeMillis() - (6L * 60 * 60 * 1000)
        val stale = """{"phase":1,"checkedIdsPhase1":[],"checkedIdsPhase2":[],"lostIds":[],"savedAt":$staleSavedAt}"""
        every { prefs.getString("night_duty_suspended", null) } returns stale
        assertTrue(viewModel.isSuspendedDataStale(prefs))

        every { prefs.getString("night_duty_suspended", null) } returns "{not valid json"
        assertTrue("Corrupt JSON must be treated as stale", viewModel.isSuspendedDataStale(prefs))

        every { prefs.getString("night_duty_suspended", null) } returns null
        assertFalse("Missing data is not stale (no resume to clear)", viewModel.isSuspendedDataStale(prefs))
    }
}
