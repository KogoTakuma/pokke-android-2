package com.kumanodormitory.pokke.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumanodormitory.pokke.ui.component.ThreeColumnSelector
import com.kumanodormitory.pokke.ui.util.SoundManager
import com.kumanodormitory.pokke.ui.viewmodel.ParcelRegisterUiState
import com.kumanodormitory.pokke.ui.viewmodel.ParcelRegisterViewModel

// 旧アプリ colors.xml から引用
private val RegisterTheme = Color(0xFFADD8E6) // register_theme: lightblue
private val RegisterHeaderFont = Color(0xFF000000)

// 荷物種別ボタンの色定義（旧アプリの配色を踏襲）
private val ParcelTypeNormalColor = Color(0xFFFFCDCD)     // data1A: 普通
private val ParcelTypeRefrigeratedColor = Color(0xFFD0D6EA) // data1B: 冷蔵
private val ParcelTypeFrozenColor = Color(0xFFD0D6EA)      // data1B: 冷凍（冷蔵と同系統）
private val ParcelTypeLargeColor = Color(0xFFFDE9CB)       // data1C: 大型
private val ParcelTypeAbsenceColor = Color(0xFFD1D1D1)     // data1E: 不在票
private val ParcelTypeOtherColor = Color(0xFFE9EBF5)       // data1D: その他

private data class ParcelTypeButton(
    val value: String,
    val label: String,
    val color: Color
)

private val parcelTypeButtons = listOf(
    ParcelTypeButton("NORMAL", "普通", ParcelTypeNormalColor),
    ParcelTypeButton("REFRIGERATED", "冷蔵", ParcelTypeRefrigeratedColor),
    ParcelTypeButton("FROZEN", "冷凍", ParcelTypeFrozenColor),
    ParcelTypeButton("LARGE", "大型", ParcelTypeLargeColor),
    ParcelTypeButton("ABSENCE_SLIP", "不在票", ParcelTypeAbsenceColor),
    ParcelTypeButton("OTHER", "その他", ParcelTypeOtherColor)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelRegisterScreen(
    viewModel: ParcelRegisterViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocks by viewModel.blocks.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val ryoseiList by viewModel.ryoseiList.collectAsState()
    val selectedBlock by viewModel.selectedBlock.collectAsState()
    val selectedRoom by viewModel.selectedRoom.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedRyosei by viewModel.selectedRyosei.collectAsState()
    val showTypeDialog by viewModel.showTypeDialog.collectAsState()
    val note by viewModel.note.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 連打防止用
    var lastRegisterClickTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ParcelRegisterUiState.Success -> {
                SoundManager.playDone(context)
                snackbarHostState.showSnackbar("登録しました")
                viewModel.resetUiState()
            }
            is ParcelRegisterUiState.Error -> {
                SoundManager.playError(context)
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "受け取り",
                        fontSize = 24.sp,
                        color = RegisterHeaderFont
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                            tint = RegisterHeaderFont
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RegisterTheme
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 40.dp, vertical = 16.dp)) {
            ThreeColumnSelector(
                blocks = blocks,
                rooms = rooms,
                ryoseiList = ryoseiList,
                onRyoseiSelected = { ryosei ->
                    // 連打防止 (400ms)
                    val now = System.currentTimeMillis()
                    if (now - lastRegisterClickTime > 400L) {
                        lastRegisterClickTime = now
                        SoundManager.playCursorRyosei(context)
                        viewModel.selectRyosei(ryosei)
                    }
                },
                selectedBlock = selectedBlock,
                onBlockSelected = { block ->
                    SoundManager.playCursorBlock(context)
                    viewModel.selectBlock(block)
                },
                selectedRoom = selectedRoom,
                onRoomSelected = { room ->
                    SoundManager.playCursorRoom(context)
                    viewModel.selectRoom(room)
                },
                showSearch = true,
                searchQuery = searchQuery,
                onSearchQueryChange = { query ->
                    if (query.isNotBlank() && query != searchQuery) {
                        SoundManager.playSearch(context)
                    }
                    viewModel.updateSearchQuery(query)
                },
                isRyoseiEnabled = { ryosei -> ryosei.leavingDate == null },
                ryoseiSuffix = { ryosei ->
                    if (ryosei.leavingDate != null) "退寮済" else null
                }
            )

            if (uiState is ParcelRegisterUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    // 荷物種別選択ダイアログ（旧アプリの SingleChoiceItems + ボタンスタイルを再現）
    if (showTypeDialog && selectedRyosei != null) {
        ParcelTypeDialog(
            ryoseiRoom = selectedRyosei!!.room,
            ryoseiName = selectedRyosei!!.name,
            note = note,
            onNoteChange = { viewModel.updateNote(it) },
            isLoading = uiState is ParcelRegisterUiState.Loading,
            onRegister = { type, noteText ->
                // 連打防止 (1000ms)
                val now = System.currentTimeMillis()
                if (now - lastRegisterClickTime > 1000L) {
                    lastRegisterClickTime = now
                    viewModel.registerParcel(type, noteText)
                }
            },
            onDismiss = { viewModel.dismissTypeDialog() }
        )
    }
}

@Composable
private fun ParcelTypeDialog(
    ryoseiRoom: String,
    ryoseiName: String,
    note: String,
    onNoteChange: (String) -> Unit,
    isLoading: Boolean,
    onRegister: (type: String, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf<ParcelTypeButton?>(null) }
    var showNoteInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "$ryoseiRoom $ryoseiName に荷物受け取りします。",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 6種別ボタンを2列×3行で配置
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in parcelTypeButtons.indices step 2) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ParcelTypeButtonItem(
                                type = parcelTypeButtons[i],
                                isSelected = selectedType == parcelTypeButtons[i],
                                onClick = {
                                    selectedType = parcelTypeButtons[i]
                                    showNoteInput = parcelTypeButtons[i].value == "OTHER"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (i + 1 < parcelTypeButtons.size) {
                                ParcelTypeButtonItem(
                                    type = parcelTypeButtons[i + 1],
                                    isSelected = selectedType == parcelTypeButtons[i + 1],
                                    onClick = {
                                        selectedType = parcelTypeButtons[i + 1]
                                        showNoteInput = parcelTypeButtons[i + 1].value == "OTHER"
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 「その他」選択時の備考入力欄
                if (showNoteInput) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        label = { Text("備考（200字以内）") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        singleLine = false
                    )
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(24.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val type = selectedType ?: return@Button
                    onRegister(type.value, if (type.value == "OTHER") note else "")
                },
                enabled = selectedType != null && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RegisterTheme
                )
            ) {
                Text(
                    text = "受け取り",
                    fontSize = 18.sp,
                    color = RegisterHeaderFont
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル", fontSize = 18.sp)
            }
        }
    )
}

@Composable
private fun ParcelTypeButtonItem(
    type: ParcelTypeButton,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp

    Box(
        modifier = modifier
            .height(56.dp)
            .background(type.color, RoundedCornerShape(8.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = type.label,
            fontSize = 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}
