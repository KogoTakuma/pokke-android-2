package com.kumanodormitory.pokke.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatParcelType(type: String): String {
    return when (type) {
        "NORMAL" -> "普通"
        "REFRIGERATED" -> "冷蔵"
        "FROZEN" -> "冷凍"
        "LARGE" -> "大型"
        "ABSENCE_SLIP" -> "不在票"
        "OTHER" -> "その他"
        else -> type
    }
}

fun formatOperationType(type: String): String {
    return when (type) {
        "REGISTER" -> "登録"
        "DELIVER" -> "引渡"
        "CANCEL_REGISTER" -> "登録取消"
        "CANCEL_DELIVER" -> "引渡取消"
        "MARK_LOST" -> "紛失の反映"
        "NIGHT_DUTY_CONFIRM" -> "泊まり事務当番"
        "DUTY_CHANGE" -> "当番交代"
        else -> type
    }
}

private val dateTimeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)

fun formatDateTime(epochMillis: Long): String {
    return dateTimeFormat.format(Date(epochMillis))
}
