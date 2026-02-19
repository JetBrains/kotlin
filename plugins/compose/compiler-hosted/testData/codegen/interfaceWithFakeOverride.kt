// DUMP_IR

// FILE: main.kt
package com.example.home

import androidx.compose.runtime.Composable
import com.example.interfaces.I

@Composable
fun foo(arg: I?) {
}

// FILE: interfaces.kt
package com.example.interfaces

interface I : IBase

interface IBase {
    fun x(): Int {
        return 1
    }
}
