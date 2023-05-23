// EXPECTED_REACHABLE_NODES: 1735
// KJS_WITH_FULL_RUNTIME
// SKIP_DCE_DRIVEN
// RUN_UNIT_TESTS

import common.*
import kotlin.test.Test

class Outer {

    val prop = "prop"

    @Test
    fun test1() {
    }

    inner class Inner {

        @Test fun innerTest() {
            call(prop + "Inner")
        }

        inner class Inneer {
            @Test fun innermostTest() {
                call(prop + "Inneer")
            }
        }
    }

    class Nested {
        @Test
        fun a() {
        }

        @Test
        fun b() {
        }

        class EvenDeeper {

            @Test
            fun c() {
            }
        }
    }

    @Test
    fun test2() {
    }

    companion object {
        @Test
        fun companionTest() {
        }

        object InnerCompanion {
            @Test
            fun innerCompanionTest() {
            }
        }
    }
}

fun box() = checkLog {
    suite("Outer") {
        test("test1")
        suite("Inner") {
            test("innerTest") {
                call("propInner")
            }
            suite("Inneer") {
                test("innermostTest") {
                    call("propInneer")
                }
            }
        }
        suite("Nested") {
            test("a")
            test("b")
            suite("EvenDeeper") {
                test("c")
            }
        }
        test("test2")
        suite("Companion") {
            test("companionTest")
            suite("InnerCompanion") {
                test("innerCompanionTest")
            }
        }
    }
}