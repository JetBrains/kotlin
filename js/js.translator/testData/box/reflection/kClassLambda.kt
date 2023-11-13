import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class CoroutineStub {
    companion object {
        fun evaluate(c: suspend () -> Unit) {
            c.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) {
                    result.getOrThrow()
                }
            })
        }
    }
}

fun testLabmdaParam(x: () -> Unit) {
    assertEquals(x::class.simpleName, "Function0")
}

fun testSuspendLambaParam(x: suspend () -> Unit) {
    assertEquals(x::class.simpleName, "Function1")
}

inline fun <reified T> testLabmdaParamGeneric(x: T) {
    assertEquals(T::class.simpleName, "Function0")
}

inline fun <reified T> testSuspendLambaParamGeneric(x: T) {
    assertEquals(T::class.simpleName, "SuspendFunction0")
}

fun box(): String {
    testLabmdaParam({})
    testSuspendLambaParam(suspend {})
    testLabmdaParamGeneric({})
    testSuspendLambaParamGeneric(suspend {})

    val sx = suspend {
        val sy = suspend {
            val sz = suspend {
                val a = {
                    val b = {
                        val c = {
                            CoroutineStub.evaluate {
                                val x = suspend {
                                    val y = suspend {
                                        val z = suspend { }
                                        assertEquals(z::class.simpleName, "Function1")

                                        assertEquals((suspend { })::class.simpleName, "Function1")
                                        assertEquals(({ })::class.simpleName, "Function0")
                                    }
                                    y()
                                    assertEquals(y::class.simpleName, "Function1")
                                }
                                x()
                                assertEquals(x::class.simpleName, "Function1")
                            }
                        }
                        c()
                        assertEquals(c::class.simpleName, "Function0")
                    }
                    b()
                    assertEquals(b::class.simpleName, "Function0")
                }
                a()
                assertEquals(a::class.simpleName, "Function0")
            }
            sz()
            assertEquals(sz::class.simpleName, "Function1")
        }
        sy()
        assertEquals(sy::class.simpleName, "Function1")
    }
    assertEquals(sx::class.simpleName, "Function1")
    CoroutineStub.evaluate(sx)

    return "OK"
}
