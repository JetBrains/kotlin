package testDslExample

import kotlin.io.*
import kotlinhack.test.*

import junit.framework.*
import junit.textui.TestRunner

public val suite : TestSuite? = testSuite<Int>("test group") {
    var staticState = 10

    setUp = {
        staticState += 10
        state = staticState
        println("test '${getName()}' started")
    }

    tearDown = {
        println("test '${getName()}' ended")
    }

    "test 1" - {
        Assert.assertEquals(state, 20)
        println("test '${getName()}': state: $state")
    }

    "test 2" - {
        assertTrue(state == 31, "message")
        println("test '${getName()}': state: $state")
    }

    "test 3" - {
        Assert.assertEquals(state, 40)
        println("test '${getName()}': state: $state")
    }

    testSuite<Any>("nested test suite") {
        setUp = {
            staticState += 10
            state = staticState
            println("nested test '${getName()}' started")
        }

        tearDown = {
            println("nested test '${getName()}' ended")
        }

        "nested test 1" - {
            println("test '${getName()}'")
        }

        "nested test 2" - {
            println("test '${getName()}'")
        }

        "nested test 3" - {
            println("test '${getName()}'")
        }
    }
}
