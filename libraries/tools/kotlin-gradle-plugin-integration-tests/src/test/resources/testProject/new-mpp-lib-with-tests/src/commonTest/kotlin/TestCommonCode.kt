package com.example.lib

import kotlin.test.Test
import kotlin.test.assertEquals

class TestCommonCode {
	@Test
	fun testId() {
		val x = 1
		val idX = id(x)
		assertEquals(x, idX)
	}

	@Test
	fun testExpectedFun() {
		expectedFun()
	}
}