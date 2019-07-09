package com.example.externalLib

fun x(): String = "x"

actual fun expectedFun() {
	println(id(x()))
}
