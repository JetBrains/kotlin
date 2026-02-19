fun isEqual(l: Any?, r: Any?) = if (l == r) true else null

inline fun test1(x: () -> Any) = x()
fun test2(x: () -> Any) = x()
inline fun test3(x: () -> Any) = x()

fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0 -> {
            isEqual(test1(::foo1), "foo1") ?: return "Fail foo1"
            isEqual(test2(::foo2), "foo2") ?: return "Fail foo2"
            isEqual(test3 { foo3() }, "foo3") ?: return "Fail foo3"
        }
        1 -> {
            isEqual(test1(::foo1), "foo1 update") ?: return "Fail foo1"
            isEqual(test2(::foo2), "foo2") ?: return "Fail foo2"
            isEqual(test3 { foo3() }, "foo3") ?: return "Fail foo3"
        }
        2 -> {
            isEqual(test1(::foo1), "foo1 update") ?: return "Fail foo1"
            isEqual(test2(::foo2), "foo2 update") ?: return "Fail foo2"
            isEqual(test3 { foo3() }, "foo3") ?: return "Fail foo3"
        }
        3 -> {
            isEqual(test1(::foo1), 77) ?: return "Fail foo1"
            isEqual(test2(::foo2), "foo2 update") ?: return "Fail foo2"
            isEqual(test3 { foo3() }, "foo3") ?: return "Fail foo3"
        }
        4 -> {
            isEqual(test1(::foo1), 77) ?: return "Fail foo1"
            isEqual(test2(::foo2), 88) ?: return "Fail foo2"
            isEqual(test3 { foo3() }, "foo3") ?: return "Fail foo3"
        }
        5, 6 -> {
            isEqual(test1(::foo1), 77) ?: return "Fail foo1"
            isEqual(test2(::foo2), 88) ?: return "Fail foo2"
            isEqual(test3 { foo3() }, "foo3 update") ?: return "Fail foo3"
        }
        7, 8 -> {
            isEqual(test1(::foo1), 99) ?: return "Fail foo1"
            isEqual(test2(::foo2), 88) ?: return "Fail foo2"
            isEqual(test3 { foo3() }, "foo3 update") ?: return "Fail foo3"
        }
        9 -> {
            isEqual(test1(::foo1), 88) ?: return "Fail foo1"
            isEqual(test2(::foo2), 88) ?: return "Fail foo2"
            isEqual(test3 { foo3() }, "foo3 update") ?: return "Fail foo3"
        }
        else -> return "Unknown"
    }
    return "OK"
}
