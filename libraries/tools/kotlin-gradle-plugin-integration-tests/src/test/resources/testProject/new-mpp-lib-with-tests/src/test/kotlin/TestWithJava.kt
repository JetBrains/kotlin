package com.example.lib

import org.junit.Test

class TestWithJava {
	@Test
	fun testJavaClass() {
		JavaClass()
		f()
	}

	companion object {
		val seesJavaTestClass = TestJava::class
	}
}