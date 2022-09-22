// DONT_TARGET_EXACT_BACKEND: JS
// GENERATE_INLINE_ANONYMOUS_FUNCTIONS

inline fun foo(l: () -> Unit) { l() }
inline fun bar(l: () -> Unit) { l() }

inline fun baz(l: () -> String) = l()

fun noninline(l: () -> String) = l()

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun inlineOnly(l: () -> String) = l()

fun test1(a: Boolean) = baz {
    foo {
        val s = "O"
        val localFun: () -> String = { s }
        if (a)
            return@baz localFun()
        else
            return@baz "K"
    }
    "Fail test1"
}

fun test2(): String {
    foo {
        bar {
            return "OK"
        }
    }
    return "Fail test2"
}

fun test3(): String {
    foo {
        bar {
            return@foo;
        }
        return "Fail"
    }
    return "OK"
}

class A(val a: Int) {

    fun test4() = baz { "$a" }

    inner class B {
        fun test5() = baz { "$a" }

        fun test6() = noninline {
            baz { "$a" }
        }
    }

    fun test7() = noninline {
        baz { "$a" }
    }

    fun test9() = baz {
        inlineOnly {
            "$a"
        }
    }
}

fun test8(a: Long = 5.seconds) = a.toString()

inline val Number.seconds: Long get() = this.toLong()

fun test10(a: Int): String {
    return baz one@ {
        baz two@ {
            baz three@ {
                when (a) {
                    1 -> return@one "1"
                    2 -> return@two "2"
                    3 -> return@two js("undefined").unsafeCast<String>() // Break the type safety intentionally.
                    4 -> return "4"
                    else -> "Fail"
                }
            } + "-three"
        } + "-two"
    } + "-one"
}

inline fun doCallAlwaysBreak(block: (i: Int)-> Int) : Int {
    var res = 0;
    for (i in 1..10) {
        try {
            block(i)
        } finally {
            break;
        }
    }
    return res
}

fun test11(): String {
    return doCallAlwaysBreak {
        return "Fail"
    }.toString()
}

fun box(): String {
    assertEquals("OK", test1(true) + test1(false))
    assertEquals("OK", test2())
    assertEquals("OK", test3())
    assertEquals("1", A(1).test4())
    assertEquals("2", A(2).B().test5())
    assertEquals("3", A(3).B().test6())
    assertEquals("4", A(4).test7())
    assertEquals("5", test8())
    assertEquals("6", A(6).test9())
    assertEquals("1-one", test10(1))
    assertEquals("2-two-one", test10(2))
    assertEquals("undefined-two-one", test10(3))
    assertEquals("4", test10(4))
    assertEquals("0", test11())
    return "OK"
}
