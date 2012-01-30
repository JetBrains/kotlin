package testDslExample

import std.io.*
import stdhack.test.*

import junit.framework.*
import junit.textui.TestRunner

public val suite : TestSuite? = testSuite<Int>("test group") {
    var staticState = 10

    setUp = {
        staticState += 10
        state = staticState
        println("test '$name' started")
    }

    tearDown = {
        println("test '$name' ended")
    }

    "test 1" - {
        Assert.assertEquals(state, 20)
        println("test '$name': state: $state")
    }

    "test 2" - {
        assert(state == 31, "message")
        println("test '$name': state: $state")
    }

    "test 3" - {
        Assert.assertEquals(state, 40)
        println("test '$name': state: $state")
    }

    testSuite<Any>("nested test suite") {
        setUp = {
            staticState += 10
            state = staticState
            println("nested test '$name' started")
        }

        tearDown = {
            println("nested test '$name' ended")
        }

        "nested test 1" - {
            println("test '$name'")
        }

        "nested test 2" - {
            println("test '$name'")
        }

        "nested test 3" - {
            println("test '$name'")
        }
    }
}
