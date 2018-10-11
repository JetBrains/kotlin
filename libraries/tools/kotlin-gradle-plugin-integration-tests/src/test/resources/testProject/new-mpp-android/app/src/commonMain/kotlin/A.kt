package com.example.app

import com.example.lib.*

expect fun f(): Unit

fun g() {
    ExpectedLibClass()
}