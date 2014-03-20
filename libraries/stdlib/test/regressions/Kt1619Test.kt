package regressions

import junit.framework.TestCase
import kotlin.test.expect

class Kt1619Test: TestCase() {

    fun doSomething(list: List<String?>): Int {
        return list.size()
    }

    fun testCollectionNotNullCanBeUsedForNullables() {
        val list: List<String> = arrayListOf("foo", "bar")
        expect(2) { doSomething(list) }
    }
}