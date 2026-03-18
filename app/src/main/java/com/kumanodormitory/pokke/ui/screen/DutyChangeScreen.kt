package com.kumanodormitory.pokke.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import com.kumanodormitory.pokke.ui.component.ThreeColumnSelector
import com.kumanodormitory.pokke.ui.util.SoundManager
import com.kumanodormitory.pokke.ui.util.debounceClickable
import com.kumanodormitory.pokke.ui.viewmodel.DutyChangeUiState
import com.kumanodormitory.pokke.ui.viewmodel.DutyChangeViewModel

// 旧アプリの色定義（activity_jimuto_change）
private val JimutoHeaderColor = Color(0xFF60DEA0)   // jimuto_theme
private val HeaderFontColor = Color.White
private val FooterColor = Color(0xFF333C5E)         // default_theme
private val FooterFontColor = Color(0xFFA9A9A9)     // default_footer_font

@Composable
fun DutyChangeScreen(
    viewModel: DutyChangeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocks by viewModel.blocks.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val ryoseiList by viewModel.ryoseiList.collectAsState()
    val selectedBlock by viewModel.selectedBlock.collectAsState()
    val selectedRoom by viewModel.selectedRoom.collectAsState()
    val selectedRyosei by viewModel.selectedRyosei.collectAsState()
    val showConfirmDialog by viewModel.showConfirmDialog.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is DutyChangeUiState.Success) {
            SoundManager.playDone(context)
            onNavigateBack()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ===== ヘッダー（旧: include_header_jimuto_change） =====
        JimutoHeader(onNavigateBack = onNavigateBack)

        // ===== コンテンツ: 3カラムセレクター =====
        // 旧: paddingHorizontal=110dp, paddingVertical=20dp
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 60.dp, vertical = 20.dp)
        ) {
            ThreeColumnSelector(
                blocks = blocks,
                rooms = rooms,
                ryoseiList = ryoseiList,
                onRyoseiSelected = { viewModel.selectRyosei(it) },
                selectedBlock = selectedBlock,
                onBlockSelected = { viewModel.selectBlock(it) },
                selectedRoom = selectedRoom,
                onRoomSelected = { viewModel.selectRoom(it) },
                showSearch = true,
                isRyoseiEnabled = { ryosei -> !isRyoseiLeft(ryosei) },
                onBlockClick = { SoundManager.playCursorBlock(context) },
                onRoomClick = { SoundManager.playCursorRoom(context) },
                onRyoseiClick = { SoundManager.playCursorRyosei(context) },
                onSearchSubmit = { SoundManager.playSearch(context) }
            )

            // ローディング表示
            if (uiState is DutyChangeUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // ===== フッター =====
        JimutoFooter()
    }

    // ===== 確認ダイアログ =====
    if (showConfirmDialog && selectedRyosei != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = {
                Text(
                    text = "事務当番交代",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${selectedRyosei!!.room}  ${selectedRyosei!!.name}\nに事務当番を交代しますか？",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDutyChange() }
                ) {
                    Text(
                        text = "交代する",
                        color = Color(0xFF60DEA0),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // ===== エラーダイアログ =====
    if (uiState is DutyChangeUiState.Error) {
        val errorMessage = (uiState as DutyChangeUiState.Error).message
        SoundManager.playError(context)
        AlertDialog(
            onDismissRequest = {},
            title = { Text("エラー") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = onNavigateBack) {
                    Text("OK")
                }
            }
        )
    }
}

// ===== ヘッダー（旧: jimuto_theme #60DEA0） =====
@Composable
private fun JimutoHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(JimutoHeaderColor)
            .padding(horizontal = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 戻るボタン
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.3f))
                .debounceClickable(1000L) { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "←",
                fontSize = 24.sp,
                color = HeaderFontColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "事務当番交代",
            color = HeaderFontColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        // 右側のバランス用スペーサー
        Spacer(modifier = Modifier.size(50.dp))
    }
}

// ===== フッター =====
@Composable
private fun JimutoFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .background(FooterColor)
            .padding(horizontal = 50.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Copyright \u00A9 2026 Kumano Dormitory IT Section",
            color = FooterFontColor,
            fontSize = 14.sp
        )
    }
}

private fun isRyoseiLeft(ryosei: RyoseiEntity): Boolean {
    val leavingDate = ryosei.leavingDate ?: return false
    return leavingDate < System.currentTimeMillis()
}
