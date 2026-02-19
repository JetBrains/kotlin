// DUMP_IR

// MODULE: anno
// MODULE_KIND: LibraryBinary
// FILE: FloatRange.kt
package com.example.anno

annotation class FloatRange(val d: Double, val d1: Double)

// MODULE: main(anno)
// FILE: Status.kt
package com.example.home

import android.graphics.Color
import androidx.compose.runtime.Composable
import com.example.anno.FloatRange

data class Status(
    val type: String,
    @FloatRange(0.0, 1.0) val progress: Float,
    val color: Color,
    val label: String,
)

class LLL

@Composable
fun LLL.toStatusList(): List<Status> {
    return emptyList()
}

// FILE: main.kt
package com.example.home

import androidx.compose.runtime.Composable

@Composable
fun ttt(lll: LLL) {
    lll.toStatusList().forEach { callMe(it) }
}

@Composable
fun callMe(s: Status) {}