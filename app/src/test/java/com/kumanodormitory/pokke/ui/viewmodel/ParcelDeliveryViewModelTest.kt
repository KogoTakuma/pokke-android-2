package com.kumanodormitory.pokke.ui.viewmodel

import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import com.kumanodormitory.pokke.data.repository.DutyPersonRepository
import com.kumanodormitory.pokke.data.repository.OperationLogRepository
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import com.kumanodormitory.pokke.data.repository.RyoseiRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParcelDeliveryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val ryoseiRepository = mockk<RyoseiRepository>()
    private val parcelRepository = mockk<ParcelRepository>()
    private val dutyPersonRepository = mockk<DutyPersonRepository>()
    private val operationLogRepository = mockk<OperationLogRepository>()
    private lateinit var viewModel: ParcelDeliveryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun makeRyosei(id: String, block: String, room: String) = RyoseiEntity(
        id = id,
        name = "テスト$id",
        nameKana = "てすと",
        nameAlphabet = "Test",
        room = room,
        block = block
    )

    private fun makeParcel(id: String, ryoseiId: String) = ParcelEntity(
        id = id,
        createdAt = 0L,
        updatedAt = 0L,
        ryoseiId = ryoseiId,
        ownerBlock = "A1",
        ownerRoomName = "A101",
        ownerName = "テスト",
        parcelType = "小荷物",
        registeredByName = "admin"
    )

    private fun createViewModel(ryosei: List<RyoseiEntity>, parcels: List<ParcelEntity>) {
        every { ryoseiRepository.getRyoseiWithParcels() } returns flowOf(ryosei)
        every { parcelRepository.getRegisteredParcels() } returns flowOf(parcels)
        every { dutyPersonRepository.getCurrentDutyPerson() } returns flowOf(null)
        viewModel = ParcelDeliveryViewModel(
            ryoseiRepository, parcelRepository, dutyPersonRepository, operationLogRepository
        )
    }

    @Test
    fun `selectRoom 通常ブロック 部屋名で寮生1件取得`() = runTest(testDispatcher) {
        val ryosei = makeRyosei("r1", "A1", "A101")
        val parcel = makeParcel("p1", "r1")
        createViewModel(listOf(ryosei), listOf(parcel))
        advanceUntilIdle()

        viewModel.selectBlock("A1")
        viewModel.selectRoom("A101")

        assertEquals(1, viewModel.uiState.value.ryoseiWithParcels.size)
        assertEquals("r1", viewModel.uiState.value.ryoseiWithParcels.first().id)
    }

    @Test
    fun `selectRoom その他ブロック選択後に部屋で寮生1件取得 致命4再現防止`() = runTest(testDispatcher) {
        val ryosei = makeRyosei("r2", "A1", "R101")
        val parcel = makeParcel("p2", "r2")
        createViewModel(listOf(ryosei), listOf(parcel))
        advanceUntilIdle()

        viewModel.selectBlock(ParcelDeliveryViewModel.BLOCK_OTHER)
        viewModel.selectRoom("R101")

        assertEquals(1, viewModel.uiState.value.ryoseiWithParcels.size)
        assertEquals("r2", viewModel.uiState.value.ryoseiWithParcels.first().id)
    }

    @Test
    fun `selectRoom 活荷物なし寮生は0件`() = runTest(testDispatcher) {
        val ryosei = makeRyosei("r3", "A1", "A102")
        createViewModel(listOf(ryosei), emptyList())
        advanceUntilIdle()

        viewModel.selectRoom("A102")

        assertEquals(0, viewModel.uiState.value.ryoseiWithParcels.size)
    }
}
