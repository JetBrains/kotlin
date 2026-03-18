fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2(1, 2) },
    "test3" to { test3(1, 2) },
    "test4" to { test4(1, 2) },
    "test5" to { test5(1, 2) },
    "test6" to { test6(2) },
)

fun test1() {
    assert(try { false } catch (e: Exception) { true })
}

fun test2(a: Int, b: Int) {
    assert(try { true; a > b } catch (e: NullPointerException) { b > a } catch (e: Exception) { b == a })
}

fun test3(a: Int, b: Int) {
    assert(try { a > b } catch (e: Exception) { b == a } finally { a < b })
}

fun test4(a: Int, b: Int) {
    assert(try { a > b } finally { a == b ; 10})
}

fun test5(a: Int, b: Int) {
    assert(try { a > b } catch (e: Exception) { b == a } finally { a < b } == (a.inc() == 2))
}

fun test6(x: Int) {
    assert(
        try {
            when {
                x == 3 -> true
                else -> false
            }
        } finally {
            1
        }
    )
}