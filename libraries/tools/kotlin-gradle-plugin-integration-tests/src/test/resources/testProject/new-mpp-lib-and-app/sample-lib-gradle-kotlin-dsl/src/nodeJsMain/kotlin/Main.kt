package com.example.lib

import com.example.lib.id
import com.example.lib.expectedFun

fun idUsage() = id("123")

actual fun expectedFun() = Unit

fun main(args: Array<String>) {
	expectedFun()
}