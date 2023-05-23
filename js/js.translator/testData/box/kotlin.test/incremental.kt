// EXPECTED_REACHABLE_NODES: 1829
// KJS_WITH_FULL_RUNTIME
// SKIP_DCE_DRIVEN
// RUN_UNIT_TESTS

// FILE: a.kt
package a

import common.*
import kotlin.test.*

class A {
    @BeforeTest
    fun before() {
        call("a.A.before")
    }

    @AfterTest
    fun after() {
        call("a.A.after")
    }

    @Test
    fun passing() {
        call("a.A.passing")
    }

    @Test
    fun failing() {
        call("a.A.failing")
        raise("a.A.failing.exception")
        call("never happens")
    }

    @Ignore
    @Test
    fun ignored() {
        call("a.A.ignored")
    }

    @Test
    fun withException() {
        call("withException")
        raise("some exception")
        call("never happens")
    }

    inner class Inner {
        @Test
        fun innerTest() {
            call("a.A.Inner.innerTest")
        }
    }

    class Nested {
        @Test
        fun nestedTest() {
            call("a.A.Nested.nestedTest")
        }
    }

    companion object {
        @Test
        fun companionTest() {
            call("a.A.companionTest")
        }
    }
}


object O {
    @Test
    fun test() {
        call("a.O.test")
    }
}

// FILE: a_a.kt
package a.a

import common.*
import kotlin.test.*

class A {
    @BeforeTest
    fun before() {
        call("a.a.A.before")
    }

    @AfterTest
    fun after() {
        call("a.a.A.after")
    }

    @Test
    fun passing() {
        call("a.a.A.passing")
    }

    @Test
    fun failing() {
        call("a.a.A.failing")
        raise("a.a.A.failing.exception")
        call("never happens")
    }

    @Ignore
    @Test
    fun ignored() {
        call("a.a.A.ignored")
    }

    @Test
    fun withException() {
        call("withException")
        raise("some exception")
        call("never happens")
    }

    inner class Inner {
        @Test
        fun innerTest() {
            call("a.a.A.Inner.innerTest")
        }
    }

    class Nested {
        @Test
        fun nestedTest() {
            call("a.a.A.Nested.nestedTest")
        }
    }

    companion object {
        @Test
        fun companionTest() {
            call("a.a.A.companionTest")
        }
    }
}

object O {
    @Test
    fun test() {
        call("a.a.O.test")
    }
}

// FILE: a_a2.kt
// RECOMPILE
package a.a

import common.*
import kotlin.test.*

class B {
    @BeforeTest
    fun before() {
        call("a.a.B.before")
    }

    @AfterTest
    fun after() {
        call("a.a.B.after")
    }

    @Test
    fun passing() {
        call("a.a.B.passing")
    }

    @Test
    fun failing() {
        call("a.a.B.failing")
        raise("a.a.B.failing.exception")
        call("never happens")
    }

    @Ignore
    @Test
    fun ignored() {
        call("a.a.B.ignored")
    }

    @Test
    fun withException() {
        call("withException")
        raise("some exception")
        call("never happens")
    }

    inner class Inner {
        @Test
        fun innerTest() {
            call("a.a.B.Inner.innerTest")
        }
    }

    class Nested {
        @Test
        fun nestedTest() {
            call("a.a.B.Nested.nestedTest")
        }
    }

    companion object {
        @Test
        fun companionTest() {
            call("a.a.B.companionTest")
        }
    }
}


object O2 {
    @Test
    fun test() {
        call("a.a.O2.test")
    }
}

// FILE: main.kt

import common.*
import kotlin.test.Test

class Simple {
    @Test fun foo() {
        call("foo")
    }
}

fun box() = checkLog(false) {
    suite("a") {
        suite("A") {
            test("passing") {
                call("a.A.before")
                call("a.A.passing")
                call("a.A.after")
            }
            test("failing") {
                call("a.A.before")
                call("a.A.failing")
                raised("a.A.failing.exception")
                call("a.A.after")
                caught("a.A.failing.exception")
            }
            test("ignored", true) {
                call("a.A.before")
                call("a.A.ignored")
                call("a.A.after")
            }
            test("withException") {
                call("a.A.before")
                call("withException")
                raised("some exception")
                call("a.A.after")
                caught("some exception")
            }
            suite("Inner") {
                test("innerTest") {
                    call("a.A.Inner.innerTest")
                }
            }
            suite("Nested") {
                test("nestedTest") {
                    call("a.A.Nested.nestedTest")
                }
            }
            suite("Companion") {
                test("companionTest") {
                    call("a.A.companionTest")
                }
            }
        }
        suite("O") {
            test("test") {
                call("a.O.test")
            }
        }
    }
    suite("a.a") {
        suite("A") {
            test("passing") {
                call("a.a.A.before")
                call("a.a.A.passing")
                call("a.a.A.after")
            }
            test("failing") {
                call("a.a.A.before")
                call("a.a.A.failing")
                raised("a.a.A.failing.exception")
                call("a.a.A.after")
                caught("a.a.A.failing.exception")
            }
            test("ignored", true) {
                call("a.a.A.before")
                call("a.a.A.ignored")
                call("a.a.A.after")
            }
            test("withException") {
                call("a.a.A.before")
                call("withException")
                raised("some exception")
                call("a.a.A.after")
                caught("some exception")
            }
            suite("Inner") {
                test("innerTest") {
                    call("a.a.A.Inner.innerTest")
                }
            }
            suite("Nested") {
                test("nestedTest") {
                    call("a.a.A.Nested.nestedTest")
                }
            }
            suite("Companion") {
                test("companionTest") {
                    call("a.a.A.companionTest")
                }
            }
        }
        suite("O") {
            test("test") {
                call("a.a.O.test")
            }
        }
        suite("B") {
            test("passing") {
                call("a.a.B.before")
                call("a.a.B.passing")
                call("a.a.B.after")
            }
            test("failing") {
                call("a.a.B.before")
                call("a.a.B.failing")
                raised("a.a.B.failing.exception")
                call("a.a.B.after")
                caught("a.a.B.failing.exception")
            }
            test("ignored", true) {
                call("a.a.B.before")
                call("a.a.B.ignored")
                call("a.a.B.after")
            }
            test("withException") {
                call("a.a.B.before")
                call("withException")
                raised("some exception")
                call("a.a.B.after")
                caught("some exception")
            }
            suite("Inner") {
                test("innerTest") {
                    call("a.a.B.Inner.innerTest")
                }
            }
            suite("Nested") {
                test("nestedTest") {
                    call("a.a.B.Nested.nestedTest")
                }
            }
            suite("Companion") {
                test("companionTest") {
                    call("a.a.B.companionTest")
                }
            }
        }
        suite("O2") {
            test("test") {
                call("a.a.O2.test")
            }
        }
    }
    suite("") {
        suite("Simple") {
            test("foo") {
                call("foo")
            }
        }
    }
}