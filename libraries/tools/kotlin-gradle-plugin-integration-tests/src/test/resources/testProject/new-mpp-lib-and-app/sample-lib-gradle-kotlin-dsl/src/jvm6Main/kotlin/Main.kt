package com.example.lib

fun x(): String = "x"

actual fun expectedFun() {
	println(id(x()))
}