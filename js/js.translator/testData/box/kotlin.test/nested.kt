// EXPECTED_REACHABLE_NODES: 1200
import kotlin.test.Test

class Outer {

    @Test
    fun test1() {
    }

    class Inner {
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