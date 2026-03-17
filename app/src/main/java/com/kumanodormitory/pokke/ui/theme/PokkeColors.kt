package com.kumanodormitory.pokke.ui.theme

import androidx.compose.ui.graphics.Color

object PokkeColors {
    // ── 基本色 ──
    val black = Color(0xFF000000)
    val white = Color(0xFFFFFFFF)
    val lightGray = Color(0xFFD3D3D3)
    val veryLightGray = Color(0xFFEEEEEE)
    val darkGray = Color(0xFFA9A9A9)
    val lightBlue = Color(0xFFADD8E6)
    val orange = Color(0xFFDAA186)

    // ── テーマ色 ──
    val defaultTheme = Color(0xFF333C5E)
    val jimutoTheme = Color(0xFF60DEA0)
    val registerTheme = Color(0xFFADD8E6)
    val releaseTheme = Color(0xFFDAA186)
    val oldnoteTheme = Color(0xFFEEEEEE)
    val nightDutyTheme = Color(0xFF4B0082)

    // ── ヘッダー/フッターフォント色 ──
    val defaultHeaderFont = Color(0xFFFFFFFF)
    val defaultHeaderFontOldnote = Color(0xFF000000)
    val defaultFooterFont = Color(0xFFA9A9A9)
    val registerHeaderFont = Color(0xFF000000)
    val registerFooterFont = Color(0xFF000000)
    val releaseHeaderFont = Color(0xFFFFFFFF)
    val releaseFooterFont = Color(0xFFFFFFFF)

    // ── ボタン色（テーマから派生） ──
    val buttonPrimary = Color(0xFF333C5E)
    val buttonRegister = Color(0xFFADD8E6)
    val buttonRelease = Color(0xFFDAA186)
    val buttonNightDuty = Color(0xFF4B0082)
    val buttonJimuto = Color(0xFF60DEA0)

    // ── 選択・ハイライト ──
    val selectedItem = Color(0xFFADD8E6)
    val proxyBackground = Color(0xFFFFC8B4)

    // ── エラー ──
    val errorText = Color(0xFFFF0000)

    // ── ログ背景 ──
    val logBackground = Color(0xFFEEEEEE)

    // ── 旧型ノート: A棟 (赤系) ──
    val titleA = Color(0xFFFF6767)
    val data1A = Color(0xFFFFCDCD)
    val data2A = Color(0xFFFF9A9A)

    // ── 旧型ノート: B棟 (青系) ──
    val titleB = Color(0xFF4472C4)
    val data1B = Color(0xFFD0D6EA)
    val data2B = Color(0xFFE0EBF5)

    // ── 旧型ノート: C棟 (黄系) ──
    val titleC = Color(0xFFF7BF45)
    val data1C = Color(0xFFFDE9CB)
    val data2C = Color(0xFFFDF4E7)

    // ── 旧型ノート: D棟/臨キャパ (緑系) ──
    val titleD = Color(0xFF70AD47)
    val data1D = Color(0xFFE9EBF5)
    val data2D = Color(0xFFECF2EA)

    // ── 旧型ノート: E/その他 (灰系) ──
    val titleE = Color(0xFFC0C0C0)
    val data1E = Color(0xFFD1D1D1)
    val data2E = Color(0xFFE1E1E1)
}
