package test.nullable

import org.junit.Test as test
import kotlin.test.*
import kotlin.nullable.*

class NullablesTest {

    test fun getOrElse() {
        val a:String? = "Test"
        val b:String? = null

        assertEquals(a.getOrElse("fail"), "Test")
        assertEquals(a.getOrElse("pass"), "pass")
    }
}
