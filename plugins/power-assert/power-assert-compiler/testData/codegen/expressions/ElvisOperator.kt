// DUMP_KT_IR

fun box(): String = runAll(
    "test1(\"TEST\", 0)" to { test1("TEST", 0) },
    "test1(null, 0)" to { test1(null, 0) },
    "test2(\"TEST\", null, 0)" to { test2("TEST", null, 0) },
    "test2(null, \"test\", 0)" to { test2(null, "test", 0) },
    "test2(null, null, 0)" to { test2(null, null, 0) },
    "test3(\"TEST\")" to { test3("TEST") },
    "test3(null)" to { test3(null) },
    "test4(null, null, null, null)" to { test4(null, null, null, null) },
    "test4(null, null, null, false)" to { test4(null, null, null, false) },
    "test4(null, null, false, null)" to { test4(null, null, false, null) },
    "test4(null, false, null, null)" to { test4(null, false, null, null) },
    "test4(false, null, null, null)" to { test4(false, null, null, null) },
)

fun test1(str: String?, default: Int) {
    assert((str?.lowercase()?.length ?: default) == 5)
}

fun test2(str: String?, fallback: String?, default: Int) {
    assert(((str?.lowercase() ?: fallback)?.length ?: default) == 5)
}

fun test3(str: String?) {
    assert((str?.lowercase()?.length ?: 0) == 5)
}

fun test4(b1: Boolean?, b2: Boolean?, b3: Boolean?, b4: Boolean?) {
    assert(b1 ?: b2 ?: b3 ?: b4 ?: false)
}
