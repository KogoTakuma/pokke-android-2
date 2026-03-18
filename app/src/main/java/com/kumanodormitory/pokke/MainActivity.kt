package com.kumanodormitory.pokke

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kumanodormitory.pokke.data.local.PokkeDatabase
import com.kumanodormitory.pokke.data.repository.DutyPersonRepository
import com.kumanodormitory.pokke.data.repository.OperationLogRepository
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import com.kumanodormitory.pokke.data.repository.RyoseiRepository
import com.kumanodormitory.pokke.ui.PokkeApp
import com.kumanodormitory.pokke.ui.theme.PokkeTheme
import com.kumanodormitory.pokke.ui.viewmodel.AdminViewModel
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

        setContent {
            PokkeTheme {
                PokkeApp(
                    homeViewModel = homeViewModel,
                    dutyChangeViewModel = dutyChangeViewModel,
                    parcelRegisterViewModel = parcelRegisterViewModel,
                    parcelDeliveryViewModel = parcelDeliveryViewModel,
                    nightDutyViewModel = nightDutyViewModel,
                    oldNotebookViewModel = oldNotebookViewModel,
                    adminViewModel = adminViewModel
                )
            }
        }
    }
}
