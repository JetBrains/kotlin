package com.example.externalLib

import com.example.externalLib.id
import com.example.externalLib.expectedFun

fun idUsage() = id("123")

actual fun expectedFun() = Unit

fun main(args: Array<String>) {
	expectedFun()
}
