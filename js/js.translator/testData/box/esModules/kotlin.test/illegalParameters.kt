// IGNORE_BACKEND: JS
// KJS_WITH_FULL_RUNTIME
// SKIP_DCE_DRIVEN
// RUN_UNIT_TESTS
// ES_MODULES

// FILE: test.kt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BadClass(id: Int) {
    @Test
    fun foo() {}
}

private class BadPrivateClass {
    @Test
    fun foo() {}
}

class BadProtectedMethodClass {
    @Test
    protected fun foo() {}
}

class BadPrimaryGoodSecondary(private val id: Int) {
    constructor(): this(3)
    @Test
    fun foo() {
        assertEquals(id, 3)
    }
}

class GoodSecondaryOnly {
    constructor() {
        triggered = 3
    }
    constructor(id: Int) {
        triggered = id
    }
    companion object {
        private var triggered = 0
    }
    @Test
    fun foo() {
        assertEquals(triggered, 3)
    }
}

class BadSecondaryOnly {
    private constructor() {}
    constructor(id: Int) {}
    @Test
    fun foo() {}
}

class BadConstructorClass private constructor() {
    @Test
    fun foo() {}
}

class BadProtectedConstructorClass protected constructor() {
    constructor(flag: Boolean): this()
    @Test
    fun foo() {}
}

class GoodClass() {
    constructor(id: Int): this()
    @Test
    fun foo() {}
}

class GoodNestedClass {
    class NestedTestClass {
        @Test
        fun foo() {}

        fun helperMethod(param: String) {}
    }
}

class BadNestedClass {
    class NestedTestClass(id: Int) {
        @Test
        fun foo() {}
    }
}

class BadMethodClass() {
    @Test
    fun foo(id: Int) {}

    @Test
    private fun ping() {}
}

// non-reachable scenarios are tested in nested.kt
class OuterWithPrivateCompanion {
    private companion object {
        object InnerCompanion {
            @Test
            fun innerCompanionTest() {
            }
        }
    }
}

class OuterWithPrivateMethod {
    companion object {
        object InnerCompanion {
            @Test
            private fun innerCompanionTest() {
            }
        }
    }
}

// FILE: box.kt
import common.*

fun box() = checkLog {
        suite("BadClass") {
            test("foo") {
                caught("Test class BadClass must declare a public or internal constructor with no explicit parameters")
            }
        }
        suite("BadPrivateClass") {
            test("foo") {
                caught("Test method BadPrivateClass::foo should have public or internal visibility, can not have parameters")
            }
        }
        suite("BadProtectedMethodClass") {
            test("foo") {
                caught("Test method BadProtectedMethodClass::foo should have public or internal visibility, can not have parameters")
            }
        }
        suite("BadPrimaryGoodSecondary") {
            test("foo")
        }
        suite("GoodSecondaryOnly") {
            test("foo")
        }
        suite("BadSecondaryOnly") {
            test("foo") {
                caught("Test class BadSecondaryOnly must declare a public or internal constructor with no explicit parameters")
            }
        }
        suite("BadConstructorClass") {
            test("foo") {
                caught("Test class BadConstructorClass must declare a public or internal constructor with no explicit parameters")
            }
        }
        suite("BadProtectedConstructorClass") {
            test("foo") {
                caught("Test class BadProtectedConstructorClass must declare a public or internal constructor with no explicit parameters")
            }
        }
        suite("GoodClass") {
            test("foo")
        }
        suite("GoodNestedClass") {
            suite("NestedTestClass") {
                test("foo")
            }
        }
        suite("BadNestedClass") {
            suite("NestedTestClass") {
                test("foo") {
                    caught("Test class BadNestedClass.NestedTestClass must declare a public or internal constructor with no explicit parameters")
                }
            }
        }
        suite("BadMethodClass") {
            test("foo") {
                caught("Test method BadMethodClass::foo should have public or internal visibility, can not have parameters")
            }
            test("ping") {
                caught("Test method BadMethodClass::ping should have public or internal visibility, can not have parameters")
            }
        }
        suite("OuterWithPrivateCompanion") {
            suite("Companion") {
                suite("InnerCompanion") {
                    test("innerCompanionTest") {
                        caught("Test method OuterWithPrivateCompanion.Companion.InnerCompanion::innerCompanionTest should have public or internal visibility, can not have parameters")
                    }
                }
            }
        }
        suite("OuterWithPrivateMethod") {
            suite("Companion") {
                suite("InnerCompanion") {
                    test("innerCompanionTest") {
                        caught("Test method OuterWithPrivateMethod.Companion.InnerCompanion::innerCompanionTest should have public or internal visibility, can not have parameters")
                    }
                }
            }
        }
}