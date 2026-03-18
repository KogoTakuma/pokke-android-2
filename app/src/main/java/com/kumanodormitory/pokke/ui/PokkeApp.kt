package com.kumanodormitory.pokke.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kumanodormitory.pokke.ui.screen.AdminScreen
import com.kumanodormitory.pokke.ui.screen.CallScreen
import com.kumanodormitory.pokke.ui.screen.DutyChangeScreen
import com.kumanodormitory.pokke.ui.screen.HomeScreen
import com.kumanodormitory.pokke.ui.screen.NightDutyScreen
import com.kumanodormitory.pokke.ui.screen.OldNotebookScreen
import com.kumanodormitory.pokke.ui.screen.ParcelDeliveryScreen
import com.kumanodormitory.pokke.ui.screen.ParcelRegisterScreen
import com.kumanodormitory.pokke.ui.viewmodel.AdminViewModel
import com.kumanodormitory.pokke.ui.viewmodel.CallViewModel
import com.kumanodormitory.pokke.ui.viewmodel.DutyChangeViewModel
import com.kumanodormitory.pokke.ui.viewmodel.HomeViewModel
import com.kumanodormitory.pokke.ui.viewmodel.NightDutyViewModel
import com.kumanodormitory.pokke.ui.viewmodel.OldNotebookViewModel
import com.kumanodormitory.pokke.ui.viewmodel.ParcelDeliveryViewModel
import com.kumanodormitory.pokke.ui.viewmodel.ParcelRegisterViewModel

@Composable
fun PokkeApp(
    homeViewModel: HomeViewModel,
    dutyChangeViewModel: DutyChangeViewModel,
    parcelRegisterViewModel: ParcelRegisterViewModel,
    parcelDeliveryViewModel: ParcelDeliveryViewModel,
    nightDutyViewModel: NightDutyViewModel,
    oldNotebookViewModel: OldNotebookViewModel,
    adminViewModel: AdminViewModel,
    callViewModel: CallViewModel
) {
    val navController = rememberNavController()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable("duty_change") {
                DutyChangeScreen(
                    viewModel = dutyChangeViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("parcel_register") {
                ParcelRegisterScreen(
                    viewModel = parcelRegisterViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("parcel_delivery") {
                ParcelDeliveryScreen(
                    viewModel = parcelDeliveryViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("night_duty") {
                NightDutyScreen(
                    viewModel = nightDutyViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("old_notebook") {
                OldNotebookScreen(
                    viewModel = oldNotebookViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("admin") {
                AdminScreen(
                    viewModel = adminViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("call") {
                CallScreen(
                    viewModel = callViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
