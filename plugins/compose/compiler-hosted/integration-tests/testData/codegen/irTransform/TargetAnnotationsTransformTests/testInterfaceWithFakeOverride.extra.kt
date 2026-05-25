package com.example.interfaces

import androidx.compose.runtime.Composable

interface I : IBase

interface IBase {
    fun x(): Int {
        return 1
    }
}
