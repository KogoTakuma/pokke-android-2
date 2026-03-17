package com.kumanodormitory.pokke.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumanodormitory.pokke.R
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.ui.util.SoundManager
import com.kumanodormitory.pokke.ui.util.debounceClickable
import com.kumanodormitory.pokke.ui.util.debounceClickableItem
import com.kumanodormitory.pokke.ui.util.formatDateTime
import com.kumanodormitory.pokke.ui.util.formatParcelType
import com.kumanodormitory.pokke.ui.viewmodel.NightDutyUiState
import com.kumanodormitory.pokke.ui.viewmodel.NightDutyViewModel

// 旧アプリ準拠の棟別カラー定義
private object BuildingColors {
    // A棟
    val titleA = Color(0xFFFF6767)
    val data1A = Color(0xFFFFCDCD)
    val data2A = Color(0xFFFF9A9A)
    // B棟
    val titleB = Color(0xFF4472C4)
    val data1B = Color(0xFFD0D6EA)
    val data2B = Color(0xFFE0EBF5)
    // C棟
    val titleC = Color(0xFFF7BF45)
    val data1C = Color(0xFFFDE9CB)
    val data2C = Color(0xFFFDF4E7)
    // 臨キャパ
    val titleD = Color(0xFF70AD47)
    val data1D = Color(0xFFE9EBF5)
    val data2D = Color(0xFFECF2EA)
}

private data class BuildingColorSet(
    val title: Color,
    val data1: Color,
    val data2: Color
)

private val buildingColorMap = mapOf(
    "A棟" to BuildingColorSet(BuildingColors.titleA, BuildingColors.data1A, BuildingColors.data2A),
    "B棟" to BuildingColorSet(BuildingColors.titleB, BuildingColors.data1B, BuildingColors.data2B),
    "C棟" to BuildingColorSet(BuildingColors.titleC, BuildingColors.data1C, BuildingColors.data2C),
    "臨キャパ" to BuildingColorSet(BuildingColors.titleD, BuildingColors.data1D, BuildingColors.data2D)
)

// 旧アプリ準拠: 各棟カラム幅 720dp
private val BUILDING_COLUMN_WIDTH: Dp = 720.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NightDutyScreen(
    viewModel: NightDutyViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showPhaseTransitionDialog by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.phase == 1) "泊まり事務当ー①現物確認"
                        else "泊まり事務当ー②荷物札確認"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4B0082), // night_duty_theme
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 説明文エリア（旧アプリ準拠）
            NightDutyExplanation(
                phase = uiState.phase,
                onPhase1Complete = {
                    if (uiState.allCheckedPhase1) {
                        showPhaseTransitionDialog = true
                        SoundManager.playCursorBlock(context)
                    }
                },
                onPhase2Complete = {
                    if (uiState.allCheckedPhase2) {
                        viewModel.completeNightDuty(onNavigateBack)
                        SoundManager.playDone(context)
                    }
                },
                enablePhase1Button = uiState.allCheckedPhase1,
                enablePhase2Button = uiState.allCheckedPhase2 && !uiState.isCompleting,
                isCompleting = uiState.isCompleting
            )

            // 旧アプリ準拠: 横スクロールで棟別カラムを並列表示
            val horizontalScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                NightDutyViewModel.BUILDING_TABS.forEach { building ->
                    val parcels = uiState.parcelsByBuilding[building] ?: emptyList()
                    val colors = buildingColorMap[building] ?: buildingColorMap["A棟"]!!

                    BuildingColumn(
                        building = building,
                        parcels = parcels,
                        colors = colors,
                        uiState = uiState,
                        onToggleCheck = { parcelId ->
                            viewModel.toggleCheck(parcelId)
                            SoundManager.playCursorBlock(context)
                        },
                        onToggleLost = viewModel::toggleLost
                    )
                }
            }

            // フッター: 確認済みカウント
            NightDutyFooterBar(uiState = uiState)
        }
    }

    // フェーズ遷移確認ダイアログ（旧アプリ準拠）
    if (showPhaseTransitionDialog) {
        AlertDialog(
            onDismissRequest = { showPhaseTransitionDialog = false },
            title = { Text("現物確認完了") },
            text = {
                Text("荷物の現物確認が完了しました。\n続いて荷物札の確認を行います。")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPhaseTransitionDialog = false
                    viewModel.advanceToPhase2()
                    SoundManager.playTransition(context)
                }) {
                    Text("荷物札確認へ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhaseTransitionDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun NightDutyExplanation(
    phase: Int,
    onPhase1Complete: () -> Unit,
    onPhase2Complete: () -> Unit,
    enablePhase1Button: Boolean,
    enablePhase2Button: Boolean,
    isCompleting: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "泊まり事務当番では、①荷物の現物確認、②荷物の札確認を行います。",
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (phase == 1) {
                    "現在の画面は①荷物の現物確認です。荷物の現物が事務室にあるか確認出来たら、リストにチェックを入れてください。\n事務室にあるすべての荷物についてチェックを入れたら、「①現物確認終了」ボタンを押してください。"
                } else {
                    "現在の画面は②荷物の札確認です。\n掛札がかかっていることを確認し、リストにチェックを入れてください。\nリストのすべての荷物についてチェックを入れたら、「②札確認終了」ボタンを押してください。"
                },
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            val lastButtonClickTime = remember { mutableLongStateOf(0L) }
            if (phase == 1) {
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastButtonClickTime.longValue >= 1000L) {
                            lastButtonClickTime.longValue = now
                            onPhase1Complete()
                        }
                    },
                    enabled = enablePhase1Button
                ) {
                    Text("①現物確認終了", fontSize = 18.sp)
                }
            }
            if (phase == 2) {
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastButtonClickTime.longValue >= 1000L) {
                            lastButtonClickTime.longValue = now
                            onPhase2Complete()
                        }
                    },
                    enabled = enablePhase2Button
                ) {
                    if (isCompleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp).width(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("②札確認終了", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildingColumn(
    building: String,
    parcels: List<ParcelEntity>,
    colors: BuildingColorSet,
    uiState: NightDutyUiState,
    onToggleCheck: (String) -> Unit,
    onToggleLost: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(BUILDING_COLUMN_WIDTH)
            .fillMaxHeight()
    ) {
        // 棟ラベル（旧アプリ準拠）
        Text(
            text = building,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // ヘッダー行（旧アプリ準拠: 棟別カラー背景）
        // 旧: 部屋(3), 名前(9), 札/種別(3.1), 最終確認日時(9), チェック(2), 紛失(6)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.title)
                .padding(vertical = 6.dp, horizontal = 1.dp)
        ) {
            HeaderCell(text = "部屋", weight = 3f)
            HeaderCell(text = "名前", weight = 9f)
            HeaderCell(text = "種別", weight = 3.1f)
            HeaderCell(text = "最終確認日時", weight = 9f)
            HeaderCell(text = "確認", weight = 2f)
            HeaderCell(text = "紛失", weight = 6f)
        }

        // 荷物リスト
        if (parcels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "荷物なし",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 20.dp)
            ) {
                itemsIndexed(parcels) { index, parcel ->
                    val isChecked = if (uiState.phase == 1) {
                        parcel.id in uiState.checkedIdsPhase1
                    } else {
                        parcel.id in uiState.checkedIdsPhase2
                    }
                    val isLost = parcel.id in uiState.lostIds

                    // 旧アプリ準拠: 交互背景色
                    val bgColor = if (index % 2 == 0) colors.data1 else colors.data2

                    ParcelRow(
                        parcel = parcel,
                        isChecked = isChecked,
                        isLost = isLost,
                        phase = uiState.phase,
                        bgColor = bgColor,
                        onToggleCheck = onToggleCheck,
                        onToggleLost = onToggleLost
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 1.dp),
        textAlign = TextAlign.Center,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ParcelRow(
    parcel: ParcelEntity,
    isChecked: Boolean,
    isLost: Boolean,
    phase: Int,
    bgColor: Color,
    onToggleCheck: (String) -> Unit,
    onToggleLost: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(bgColor)
            .padding(horizontal = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 部屋 (weight=3)
        Text(
            text = parcel.ownerRoomName,
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // 名前 (weight=9)
        Text(
            text = parcel.ownerName,
            modifier = Modifier.weight(9f),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // 種別 (weight=3.1)
        Text(
            text = formatParcelType(parcel.parcelType),
            modifier = Modifier.weight(3.1f),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // 最終確認日時 (weight=9)
        Text(
            text = parcel.lastConfirmedAt?.let { formatDateTime(it) + " 確認済み" } ?: "未チェック",
            modifier = Modifier.weight(9f),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // チェックボックス (weight=2)
        Box(
            modifier = Modifier.weight(2f),
            contentAlignment = Alignment.Center
        ) {
            val lastCheckTime = remember { mutableLongStateOf(0L) }
            Checkbox(
                checked = isChecked,
                onCheckedChange = {
                    val now = System.currentTimeMillis()
                    if (now - lastCheckTime.longValue >= 400L) {
                        lastCheckTime.longValue = now
                        onToggleCheck(parcel.id)
                    }
                }
            )
        }
        // 紛失エリア (weight=6): テキスト + Switch
        Row(
            modifier = Modifier.weight(6f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isLost) "紛失中" else "荷物あり",
                fontSize = 14.sp,
                color = if (isLost) Color.Red else Color.Unspecified
            )
            Spacer(modifier = Modifier.width(4.dp))
            Switch(
                checked = isLost,
                onCheckedChange = { onToggleLost(parcel.id) },
                enabled = phase == 1 // フェーズ2では紛失トグル無効（旧アプリ準拠）
            )
        }
    }
}

@Composable
private fun NightDutyFooterBar(uiState: NightDutyUiState) {
    HorizontalDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val totalCount = uiState.parcelsByBuilding.values.flatten().size
        val checkedCount = if (uiState.phase == 1) {
            uiState.checkedIdsPhase1.size
        } else {
            uiState.checkedIdsPhase2.size
        }
        val lostCount = uiState.lostIds.size

        Text(
            text = "確認済み: $checkedCount / $totalCount",
            style = MaterialTheme.typography.bodyMedium
        )
        if (lostCount > 0) {
            Text(
                text = "紛失: $lostCount 件",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
