// DUMP_KT_IR

fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2(1, 2) },
    "test2" to { test2(1, 1) },
    "test3" to { test3() },
)

fun test1() {
    assert(try { false } catch (_: Throwable) { true } finally { true })
}

fun test2(a: Int, b: Int) {
    assert(a == b && try { false } catch (_: Throwable) { true } finally { true })
}

fun test3() {
    assert(try { throw Exception("error") } catch (_: Throwable) { false } finally { true })
}
