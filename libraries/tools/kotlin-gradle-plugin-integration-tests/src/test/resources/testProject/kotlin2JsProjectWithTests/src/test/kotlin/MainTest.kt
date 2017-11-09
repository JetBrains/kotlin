package test

import org.junit.Test
import example.MyProductionClass

class MyTest {
	@Test
    fun mySimpleTest() {
        MyProductionClass().i = 10
    }
}
