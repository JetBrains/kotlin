package com.example.app

import com.example.lib.*

actual fun f() { }

object Main {
	@JvmStatic
	fun main(args: Array<String>) {
		f()
		g()

		ExpectedLibClass()
		JvmLibClass()
	}
}