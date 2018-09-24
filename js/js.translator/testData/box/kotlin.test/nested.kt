// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1367
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
            @Test fun inneerTest() {
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
                test("inneerTest") {
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