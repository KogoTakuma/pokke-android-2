package com.kumanodormitory.pokke

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kumanodormitory.pokke.data.local.PokkeDatabase
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import com.kumanodormitory.pokke.data.remote.PokkeApiClient
import com.kumanodormitory.pokke.data.repository.DutyPersonRepository
import com.kumanodormitory.pokke.data.repository.OperationLogRepository
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import com.kumanodormitory.pokke.data.repository.RyoseiRepository
import com.kumanodormitory.pokke.sync.ParcelSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.kumanodormitory.pokke.ui.PokkeApp
import com.kumanodormitory.pokke.ui.theme.PokkeTheme
import com.kumanodormitory.pokke.ui.viewmodel.AdminViewModel
import com.kumanodormitory.pokke.ui.viewmodel.CallViewModel
import com.kumanodormitory.pokke.ui.viewmodel.DutyChangeViewModel
import com.kumanodormitory.pokke.ui.viewmodel.HomeViewModel
import com.kumanodormitory.pokke.ui.viewmodel.NightDutyViewModel
import com.kumanodormitory.pokke.ui.viewmodel.OldNotebookViewModel
import com.kumanodormitory.pokke.ui.viewmodel.ParcelDeliveryViewModel
import com.kumanodormitory.pokke.ui.viewmodel.ParcelRegisterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()

        // ナビゲーションバー（△○□）とステータスバーを非表示（API 28対応）
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.statusBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val db = PokkeDatabase.getInstance(this)

        // DAOs
        val ryoseiDao = db.ryoseiDao()
        val parcelDao = db.parcelDao()
        val dutyPersonDao = db.dutyPersonDao()
        val operationLogDao = db.operationLogDao()

        // Repositories
        val ryoseiRepository = RyoseiRepository(ryoseiDao)
        val parcelRepository = ParcelRepository(parcelDao)
        val dutyPersonRepository = DutyPersonRepository(dutyPersonDao)
        val operationLogRepository = OperationLogRepository(operationLogDao)

        // ViewModels (DI導入時に差し替え予定)
        val homeViewModel = HomeViewModel(operationLogRepository, dutyPersonRepository, parcelRepository)
        val dutyChangeViewModel = DutyChangeViewModel(ryoseiRepository, dutyPersonRepository, operationLogRepository)
        val parcelRegisterViewModel = ParcelRegisterViewModel(parcelRepository, ryoseiRepository, dutyPersonRepository, operationLogRepository)
        val parcelDeliveryViewModel = ParcelDeliveryViewModel(ryoseiRepository, parcelRepository, dutyPersonRepository, operationLogRepository)
        val nightDutyViewModel = NightDutyViewModel(parcelRepository, operationLogRepository, dutyPersonRepository)
        val oldNotebookViewModel = OldNotebookViewModel(parcelRepository)
        val adminViewModel = AdminViewModel(parcelRepository, ryoseiRepository, operationLogRepository)
        val callViewModel = CallViewModel(ryoseiRepository)

        // WorkManager: 荷物バッチ同期（15分周期）
        val syncRequest = PeriodicWorkRequestBuilder<ParcelSyncWorker>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "parcel_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        // 起動時の寮生差分同期（バックグラウンド）
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = getSharedPreferences("pokke_sync", MODE_PRIVATE)
                val lastSync = prefs.getString("lastRyoseiSyncAt", null)
                val response = PokkeApiClient.service.getRyosei(updatedAfter = lastSync)
                if (response.isSuccessful) {
                    val body = response.body() ?: return@launch
                    val entities = body.ryosei.map { dto ->
                        RyoseiEntity(
                            id = dto.id, name = dto.name, nameKana = dto.nameKana,
                            nameAlphabet = dto.nameAlphabet, room = dto.room,
                            block = dto.block, leavingDate = dto.leavingDate,
                            discordStatus = dto.discordStatus
                        )
                    }
                    ryoseiRepository.insertAll(entities)
                    prefs.edit()
                        .putString("lastRyoseiSyncAt", System.currentTimeMillis().toString())
                        .apply()
                }
            } catch (_: Exception) {
                // ネットワークエラーは無視（次回起動時にリトライ）
            }
        }

        setContent {
            PokkeTheme {
                PokkeApp(
                    homeViewModel = homeViewModel,
                    dutyChangeViewModel = dutyChangeViewModel,
                    parcelRegisterViewModel = parcelRegisterViewModel,
                    parcelDeliveryViewModel = parcelDeliveryViewModel,
                    nightDutyViewModel = nightDutyViewModel,
                    oldNotebookViewModel = oldNotebookViewModel,
                    adminViewModel = adminViewModel,
                    callViewModel = callViewModel
                )
            }
        }
    }
}
