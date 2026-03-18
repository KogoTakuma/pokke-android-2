package com.kumanodormitory.pokke.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.ui.util.formatDateTime
import com.kumanodormitory.pokke.ui.util.formatParcelType
import com.kumanodormitory.pokke.ui.viewmodel.AdminUiState
import com.kumanodormitory.pokke.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("管理用画面") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (!uiState.isAuthenticated) {
            PasswordAuthScreen(
                passwordError = uiState.passwordError,
                onAuthenticate = { viewModel.authenticate(it) },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            AdminMenuContent(
                uiState = uiState,
                onSyncClick = { viewModel.showSyncSnackbar() },
                onConfirmLost = { viewModel.confirmLost(it) },
                onGenerateSeed = { viewModel.generateSeedData() },
                onDeleteSeed = { viewModel.deleteSeedData() },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun PasswordAuthScreen(
    passwordError: String?,
    onAuthenticate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "管理者認証",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "本当に実行する場合はパスワードを入力してください。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("パスワード") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onAuthenticate(password) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("認証")
                }
            }
        }
    }
}

@Composable
private fun AdminMenuContent(
    uiState: AdminUiState,
    onSyncClick: () -> Unit,
    onConfirmLost: (String) -> Unit,
    onGenerateSeed: () -> Unit,
    onDeleteSeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 左カラム: 同期 & シードデータ管理
        Column(
            modifier = Modifier
                .width(300.dp)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "システム管理",
                style = MaterialTheme.typography.titleMedium
            )
            
            // 同期セクション
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "サーバー同期", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "同期機能は未実装です。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onSyncClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF60DEA0))
                    ) {
                        Text("手動同期実行", color = Color.White)
                    }
                }
            }

            // シードデータセクション
            Card {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "開発用データ (Seed)", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onGenerateSeed,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("シードデータ投入(100名)")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onDeleteSeed,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("シードデータのみ削除")
                    }
                }
            }
        }

        // 中カラム: 同期ステータス
        Column(
            modifier = Modifier
                .width(250.dp)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "同期ステータス",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "最終同期: 未実行\nステータス: ローカルのみ",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }

        // 右カラム: 紛失荷物管理
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "紛失荷物管理",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.lostParcels.isEmpty()) {
                Text(
                    text = "紛失フラグのついた荷物はありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.lostParcels, key = { it.id }) { parcel ->
                        LostParcelItem(
                            parcel = parcel,
                            onConfirmLost = onConfirmLost
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LostParcelItem(
    parcel: ParcelEntity,
    onConfirmLost: (String) -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${parcel.ownerRoomName} ${parcel.ownerName}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "種別: ${formatParcelType(parcel.parcelType)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "登録: ${formatDateTime(parcel.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF2222)
                )
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("紛失確定", color = Color.White)
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("紛失確定") },
            text = {
                Text(
                    "本当に実行する場合は「確定」を押してください。\n\n" +
                            "${parcel.ownerRoomName} ${parcel.ownerName} の荷物（${formatParcelType(parcel.parcelType)}）" +
                            "を紛失確定します。この操作は取り消せません。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmLost(parcel.id)
                        showConfirmDialog = false
                    }
                ) {
                    Text("確定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}
