package test

import example.MyProductionClass
import kotlin.test.Test

class MainIT {
    @Test
    fun mySimpleTest() {
        listOf(1, 2, 3).forEach { // check that stdlib is available
            MyProductionClass().i = it // check that production code is available
        }
    }
}
