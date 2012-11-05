package test.nullable

import org.junit.Test as test
import kotlin.test.*
import kotlin.nullable.*

class NullablesTest {

    test fun getOrElseWithValue() {
        val a:String? = "Test"
        val b:String? = null

        assertEquals(a.getOrElse("fail"), "Test")
        assertEquals(a.getOrElse("pass"), "pass")
    }

    test fun getOrElseWithLambda() {
        val a:String? = "Test"
        val b:String? = null
        var counter = 0

        assertEquals(a.getOrElse( { counter = counter + 1; "fail" } ), "Test")
        assertEquals(counter, 0)
        assertEquals(a.getOrElse( { counter = counter + 1; "pass" } ), "pass")
        assertEquals(counter, 1)
    }
}
