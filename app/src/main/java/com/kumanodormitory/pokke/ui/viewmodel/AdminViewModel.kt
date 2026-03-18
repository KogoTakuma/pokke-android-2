package com.kumanodormitory.pokke.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import com.kumanodormitory.pokke.data.remote.PokkeApiClient
import com.kumanodormitory.pokke.data.repository.OperationLogRepository
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import com.kumanodormitory.pokke.data.repository.RyoseiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val isAuthenticated: Boolean = false,
    val lostParcels: List<ParcelEntity> = emptyList(),
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
)

class AdminViewModel(
    private val parcelRepository: ParcelRepository,
    private val ryoseiRepository: RyoseiRepository,
    private val operationLogRepository: OperationLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    companion object {
        private const val ADMIN_PASSWORD = "PassworD"
    }

    fun authenticate(password: String) {
        if (password == ADMIN_PASSWORD) {
            _uiState.value = _uiState.value.copy(
                isAuthenticated = true,
                passwordError = null
            )
            loadLostParcels()
        } else {
            _uiState.value = _uiState.value.copy(
                passwordError = "パスワードが正しくありません"
            )
        }
    }

    private fun loadLostParcels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                parcelRepository.getLostParcels().collect { parcels ->
                    _uiState.value = _uiState.value.copy(
                        lostParcels = parcels,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun confirmLost(parcelId: String) {
        viewModelScope.launch {
            try {
                parcelRepository.markLost(parcelId)
                operationLogRepository.addLog(
                    type = "MARK_LOST",
                    parcelId = parcelId,
                    operatedByName = null,
                    metadata = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "紛失確定に失敗しました: ${e.message}"
                )
            }
        }
    }

    fun generateSeedData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val seedRyoseiList = buildSeedRyoseiList()
                ryoseiRepository.insertAll(seedRyoseiList)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "シードデータを${seedRyoseiList.size}件生成しました"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "シードデータ生成に失敗しました: ${e.message}"
                )
            }
        }
    }

    private fun buildSeedRyoseiList(): List<RyoseiEntity> {
        // 熊野寮の全部屋定義 (name, gender: 1=男, 2=女, location, capacity)
        data class RoomDef(val name: String, val gender: Int, val location: Int, val capacity: Int)

        val rooms = listOf(
            // A棟1階
            RoomDef("A100", 1, 1, 1),
            RoomDef("A101", 1, 1, 4), RoomDef("A102", 1, 1, 4),
            RoomDef("A103", 1, 1, 4), RoomDef("A104", 1, 1, 4),
            RoomDef("A105", 1, 1, 4), RoomDef("A106", 1, 1, 4),
            RoomDef("A107", 1, 1, 4), RoomDef("A108", 1, 1, 4),
            RoomDef("A109", 1, 1, 4), RoomDef("A110", 1, 1, 4),
            RoomDef("A111", 1, 1, 4),
            // A棟2階
            RoomDef("A201", 1, 2, 4), RoomDef("A202", 1, 2, 4),
            RoomDef("A203", 2, 2, 4), RoomDef("A204", 2, 2, 4),
            RoomDef("A205", 1, 2, 4), RoomDef("A206", 1, 2, 4),
            RoomDef("A207", 1, 2, 4), RoomDef("A208", 1, 2, 4),
            RoomDef("A209", 1, 2, 4), RoomDef("A210", 2, 2, 4),
            RoomDef("A211", 1, 2, 4),
            // A棟3階
            RoomDef("A301", 1, 3, 4), RoomDef("A302", 1, 3, 4),
            RoomDef("A303", 1, 3, 4), RoomDef("A304", 2, 3, 4),
            RoomDef("A305", 2, 3, 4), RoomDef("A306", 1, 3, 4),
            RoomDef("A307", 1, 3, 4), RoomDef("A308", 1, 3, 4),
            RoomDef("A309", 1, 3, 4), RoomDef("A310", 1, 3, 4),
            RoomDef("A311", 1, 3, 4),
            // A棟4階
            RoomDef("A401", 1, 4, 4), RoomDef("A402", 1, 4, 4),
            RoomDef("A403", 1, 4, 4), RoomDef("A404", 2, 4, 4),
            RoomDef("A405", 2, 4, 4), RoomDef("A406", 1, 4, 4),
            RoomDef("A407", 2, 4, 4), RoomDef("A408", 1, 4, 4),
            RoomDef("A409", 1, 4, 4), RoomDef("A410", 2, 4, 4),
            RoomDef("A411", 1, 4, 4),
            // B棟1階
            RoomDef("B100", 1, 5, 4), RoomDef("B101", 1, 5, 4),
            RoomDef("B102", 1, 5, 4), RoomDef("B103", 1, 5, 4),
            RoomDef("B104", 1, 5, 4), RoomDef("B105", 1, 5, 4),
            RoomDef("B106", 1, 5, 4), RoomDef("B107", 1, 5, 1),
            // B棟2階
            RoomDef("B201", 1, 6, 4), RoomDef("B202", 1, 6, 4),
            RoomDef("B203", 1, 6, 4), RoomDef("B204", 1, 6, 4),
            RoomDef("B205", 1, 6, 4), RoomDef("B206", 2, 6, 4),
            RoomDef("B207", 2, 6, 4), RoomDef("B208", 1, 6, 4),
            RoomDef("B209", 1, 6, 4), RoomDef("B210", 1, 6, 4),
            RoomDef("B211", 2, 6, 4), RoomDef("B212", 2, 6, 4),
            // B棟3階
            RoomDef("B301", 2, 7, 4), RoomDef("B302", 2, 7, 4),
            RoomDef("B303", 2, 7, 4), RoomDef("B304", 2, 7, 4),
            RoomDef("B305", 1, 7, 4), RoomDef("B306", 1, 7, 4),
            RoomDef("B307", 1, 7, 4), RoomDef("B308", 1, 7, 4),
            RoomDef("B309", 1, 7, 4), RoomDef("B310", 1, 7, 4),
            RoomDef("B311", 1, 7, 4), RoomDef("B312", 1, 7, 4),
            // B棟4階
            RoomDef("B401", 1, 8, 4), RoomDef("B402", 1, 8, 4),
            RoomDef("B403", 1, 8, 4), RoomDef("B404", 1, 8, 4),
            RoomDef("B405", 1, 8, 4), RoomDef("B406", 1, 8, 4),
            RoomDef("B407", 1, 8, 4), RoomDef("B408", 1, 8, 4),
            RoomDef("B409", 1, 8, 4), RoomDef("B410", 2, 8, 4),
            RoomDef("B411", 2, 8, 4), RoomDef("B412", 1, 8, 4),
            // C棟1階（地下含む）
            RoomDef("C地下", 1, 9, 1),
            RoomDef("C101", 1, 9, 6), RoomDef("C103", 1, 9, 6),
            RoomDef("C105", 1, 9, 6), RoomDef("C107", 1, 9, 6),
            // C棟2階
            RoomDef("C201", 1, 10, 6),
            RoomDef("C203", 2, 10, 6), RoomDef("C205", 2, 10, 6),
            // C棟3階
            RoomDef("C301", 2, 11, 6), RoomDef("C302", 1, 11, 6),
            RoomDef("C303", 1, 11, 6), RoomDef("C304", 1, 11, 6),
            RoomDef("C305", 1, 11, 6),
            // C棟4階
            RoomDef("C401", 1, 12, 6), RoomDef("C402", 1, 12, 6),
            RoomDef("C403", 1, 12, 6), RoomDef("C404", 2, 12, 6),
            RoomDef("C109", 1, 12, 6),
            // 仮部屋
            RoomDef("旧会議室", 1, 13, 4), RoomDef("旧印刷室", 1, 13, 4),
            RoomDef("図書室", 1, 13, 4), RoomDef("B地下踊り場", 1, 13, 4),
            RoomDef("第二音楽室", 1, 13, 4)
        )

        val locationToBlock = mapOf(
            1 to "A1", 2 to "A2", 3 to "A3", 4 to "A4",
            5 to "B1", 6 to "B2", 7 to "B3", 8 to "B4",
            9 to "C1", 10 to "C2", 11 to "C3", 12 to "C4",
            13 to "仮"
        )

        // 名字リスト
        val surnames = listOf("田中", "山田", "佐藤", "鈴木", "高橋", "渡辺", "伊藤", "中村", "小林", "加藤",
            "吉田", "山本", "松本", "井上", "木村", "林", "斎藤", "清水", "山口", "森",
            "池田", "橋本", "石川", "前田", "藤田", "小川", "後藤", "岡田", "長谷川", "村上")
        val surnamesKana = listOf("たなか", "やまだ", "さとう", "すずき", "たかはし", "わたなべ", "いとう", "なかむら", "こばやし", "かとう",
            "よしだ", "やまもと", "まつもと", "いのうえ", "きむら", "はやし", "さいとう", "しみず", "やまぐち", "もり",
            "いけだ", "はしもと", "いしかわ", "まえだ", "ふじた", "おがわ", "ごとう", "おかだ", "はせがわ", "むらかみ")
        val surnamesRomaji = listOf("Tanaka", "Yamada", "Sato", "Suzuki", "Takahashi", "Watanabe", "Ito", "Nakamura", "Kobayashi", "Kato",
            "Yoshida", "Yamamoto", "Matsumoto", "Inoue", "Kimura", "Hayashi", "Saito", "Shimizu", "Yamaguchi", "Mori",
            "Ikeda", "Hashimoto", "Ishikawa", "Maeda", "Fujita", "Ogawa", "Goto", "Okada", "Hasegawa", "Murakami")

        // 男性名リスト
        val maleNames = listOf("太郎", "翔太", "大輝", "健太", "拓也", "直人", "雄介", "誠", "大地", "隆",
            "勇気", "学", "修", "浩", "剛", "翔", "蓮", "悠斗", "陸", "颯太")
        val maleNamesKana = listOf("たろう", "しょうた", "だいき", "けんた", "たくや", "なおと", "ゆうすけ", "まこと", "だいち", "たかし",
            "ゆうき", "まなぶ", "おさむ", "ひろし", "つよし", "しょう", "れん", "はると", "りく", "そうた")
        val maleNamesRomaji = listOf("Taro", "Shota", "Daiki", "Kenta", "Takuya", "Naoto", "Yusuke", "Makoto", "Daichi", "Takashi",
            "Yuki", "Manabu", "Osamu", "Hiroshi", "Tsuyoshi", "Sho", "Ren", "Haruto", "Riku", "Sota")

        // 女性名リスト
        val femaleNames = listOf("花子", "美咲", "陽子", "由美", "愛", "葵", "明日香", "結衣", "さくら", "智子",
            "恵子", "麻衣", "真由", "彩", "凛", "七海", "遥", "楓", "茜", "瑠奈")
        val femaleNamesKana = listOf("はなこ", "みさき", "ようこ", "ゆみ", "あい", "あおい", "あすか", "ゆい", "さくら", "ともこ",
            "けいこ", "まい", "まゆ", "あや", "りん", "ななみ", "はるか", "かえで", "あかね", "るな")
        val femaleNamesRomaji = listOf("Hanako", "Misaki", "Yoko", "Yumi", "Ai", "Aoi", "Asuka", "Yui", "Sakura", "Tomoko",
            "Keiko", "Mai", "Mayu", "Aya", "Rin", "Nanami", "Haruka", "Kaede", "Akane", "Runa")

        val result = mutableListOf<RyoseiEntity>()
        var maleIdx = 0
        var femaleIdx = 0

        for (room in rooms) {
            val block = locationToBlock[room.location] ?: "不明"
            val residentsCount = if (room.capacity <= 1) 1 else 2

            repeat(residentsCount) { i ->
                val isMale = room.gender == 1
                val idx = if (isMale) maleIdx else femaleIdx

                val sIdx = idx % surnames.size
                val givenNames = if (isMale) maleNames else femaleNames
                val givenNamesKana = if (isMale) maleNamesKana else femaleNamesKana
                val givenNamesRomaji = if (isMale) maleNamesRomaji else femaleNamesRomaji
                val gIdx = idx % givenNames.size

                result.add(
                    RyoseiEntity(
                        id = "seed-${room.name}-$i",
                        name = "${surnames[sIdx]} ${givenNames[gIdx]}",
                        nameKana = "${surnamesKana[sIdx]} ${givenNamesKana[gIdx]}",
                        nameAlphabet = "${givenNamesRomaji[gIdx]} ${surnamesRomaji[sIdx]}",
                        room = room.name,
                        block = block
                    )
                )

                if (isMale) maleIdx++ else femaleIdx++
            }
        }

        return result
    }

    fun deleteSeedData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                ryoseiRepository.deleteSeedData()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "シードデータを削除しました"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "シードデータ削除に失敗しました: ${e.message}"
                )
            }
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, snackbarMessage = null)
            try {
                val response = PokkeApiClient.service.getRyosei()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val entities = body.ryosei.map { dto ->
                            RyoseiEntity(
                                id = dto.id, name = dto.name, nameKana = dto.nameKana,
                                nameAlphabet = dto.nameAlphabet, room = dto.room,
                                block = dto.block, leavingDate = dto.leavingDate,
                                discordStatus = dto.discordStatus
                            )
                        }
                        ryoseiRepository.replaceAll(entities)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            snackbarMessage = "寮生データを${entities.size}件同期しました"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        snackbarMessage = "同期失敗: HTTP ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "同期失敗: ${e.message}"
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
