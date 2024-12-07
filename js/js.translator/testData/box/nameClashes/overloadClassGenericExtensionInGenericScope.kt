class Receiver()

open class A1

open class A2: A1()

open class A3: A2()

class GenericScope<E : A1>() {
    class GenericSubScope<E : A2> {
        fun <T : E> Receiver.testOverload(e: T) = "SubScope"
    }
    fun <T : E> Receiver.testOverload(e: T) = "Scope"
}

class GenericScopeWithOverloads<E : A1>() {
    class GenericSubScopeWithOverloads<E : A2> {
        fun <T : E> Receiver.testOverload(e: T) = "SubScope"
        fun <T : A3> Receiver.testOverload(e: T) = "SubScope A3"
    }
    fun <T : E> Receiver.testOverload(e: T) = "Scope"
    fun <T : A2> Receiver.testOverload(e: T) = "Scope A2"
    fun <T : A3> Receiver.testOverload(e: T) = "Scope A3"
}

val r = Receiver()

fun testGenericScope() {
    GenericScope<A1>().apply {
        assertEquals("Scope", r.testOverload(A1()))
        assertEquals("Scope", r.testOverload(A2()))
        assertEquals("Scope", r.testOverload(A3()))

        GenericScope.GenericSubScope<A2>().apply {
            assertEquals("Scope", r.testOverload(A1()))
            assertEquals("SubScope", r.testOverload(A2()))
            assertEquals("SubScope", r.testOverload(A3()))
        }

        GenericScope.GenericSubScope<A3>().apply {
            assertEquals("Scope", r.testOverload(A1()))
            assertEquals("Scope", r.testOverload(A2()))
            assertEquals("SubScope", r.testOverload(A3()))
        }
    }

    GenericScope<A2>().apply {
        assertEquals("Scope", r.testOverload(A2()))
        assertEquals("Scope", r.testOverload(A3()))

        GenericScope.GenericSubScope<A2>().apply {
            assertEquals("SubScope", r.testOverload(A2()))
            assertEquals("SubScope", r.testOverload(A3()))
        }

        GenericScope.GenericSubScope<A3>().apply {
            assertEquals("Scope", r.testOverload(A2()))
            assertEquals("SubScope", r.testOverload(A3()))
        }
    }
}

fun testGenericScopeWithOverloads() {
    GenericScopeWithOverloads<A1>().apply {
        assertEquals("Scope", r.testOverload(A1()))
        assertEquals("Scope A2", r.testOverload(A2()))
        assertEquals("Scope A3", r.testOverload(A3()))

        GenericScopeWithOverloads.GenericSubScopeWithOverloads<A2>().apply {
            assertEquals("Scope", r.testOverload(A1()))
            assertEquals("SubScope", r.testOverload(A2()))
            assertEquals("SubScope A3", r.testOverload(A3()))
        }

        GenericScopeWithOverloads.GenericSubScopeWithOverloads<A3>().apply {
            assertEquals("Scope", r.testOverload(A1()))
            assertEquals("Scope A2", r.testOverload(A2()))
        }
    }

    GenericScopeWithOverloads<A2>().apply {
        assertEquals("Scope A3", r.testOverload(A3()))

        GenericScopeWithOverloads.GenericSubScopeWithOverloads<A2>().apply {
            assertEquals("SubScope", r.testOverload(A2()))
            assertEquals("SubScope A3", r.testOverload(A3()))
        }
    }
}

fun box(): String {
    testGenericScope()
    testGenericScopeWithOverloads()

    return "OK"
}
