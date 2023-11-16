// EXPECTED_REACHABLE_NODES: 1719
// KJS_WITH_FULL_RUNTIME
// SKIP_DCE_DRIVEN
// RUN_UNIT_TESTS
// ES_MODULES

// FILE: test.kt
import common.call
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest

interface TestyInterface {
    @Test
    fun someVarTest() {
        call("TestyInterface.someVarTest")
    }
}

abstract class AbstractTest : TestyInterface {
    @Test abstract fun abstractTest()

    @Test
    fun someTest() {
        call("AbstractTest.someTest")
    }
}

interface BeforeAfterInterface {
    @BeforeTest
    @AfterTest
    fun beforeAfter() {
        call("beforeAfter")
    }
}


class InheritedTest : AbstractTest(), BeforeAfterInterface {
    @Test override fun abstractTest() {
        call("InheritedTest.abstractTest")
    }
}

// FILE: box.kt
import common.*

fun box() = checkLog() {
    suite("InheritedTest") {
        test("abstractTest") {
            call("beforeAfter")
            call("InheritedTest.abstractTest")
            call("beforeAfter")
        }
        test("someTest") {
            call("beforeAfter")
            call("AbstractTest.someTest")
            call("beforeAfter")
        }
        test("someVarTest") {
            call("beforeAfter")
            call("TestyInterface.someVarTest")
            call("beforeAfter")
        }
    }
}