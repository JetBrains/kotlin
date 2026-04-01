package test

import kotlin.test.Test
import example.MyProductionClass

class MyTest {
	@Test
    fun mySimpleTest() {
        MyProductionClass().i = 10
    }
}
