package com.kumanodormitory.pokke.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun Modifier.debounceClickable(
    debounceInterval: Long = 1000L,
    onClick: () -> Unit
): Modifier = composed {
    val lastClickTime = remember { mutableLongStateOf(0L) }
    this.clickable {
        val now = System.currentTimeMillis()
        if (now - lastClickTime.longValue >= debounceInterval) {
            lastClickTime.longValue = now
            onClick()
        }
    }
}

fun Modifier.debounceClickableItem(
    debounceInterval: Long = 400L,
    onClick: () -> Unit
): Modifier = composed {
    val lastClickTime = remember { mutableLongStateOf(0L) }
    this.clickable {
        val now = System.currentTimeMillis()
        if (now - lastClickTime.longValue >= debounceInterval) {
            lastClickTime.longValue = now
            onClick()
        }
    }
}
