package test.collections

import kotlin.test.*
import org.junit.Test as test

class ArraysJVMTest {

    test fun orEmptyNull() {
        val x: Array<String>? = null
        val y: Array<out String>? = null
        val xArray = x.orEmpty()
        val yArray = y.orEmpty()
        expect(0) { xArray.size() }
        expect(0) { yArray.size() }
    }

    test fun orEmptyNotNull() {
        val x: Array<String>? = arrayOf("1", "2")
        val xArray = x.orEmpty()
        expect(2) { xArray.size() }
        expect("1") { xArray[0] }
        expect("2") { xArray[1] }
    }
}
