package com.example.app

import com.example.lib.*

actual fun f() { }

fun main(args: Array<String>) {
	f()
	g()
	ExpectedLibClass()
	JsLibClass()
}