fun isEqual(l: Any?, r: Any?) = if (l == r) true else null

fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0 -> {
            isEqual(Demo("test1").foo(), "foo test1") ?: return "Fail function"
            isEqual(Demo("test2").foo_inline(), "inline foo test2") ?: return "Fail inline function"
            isEqual(Demo("test3").field1, "field1") ?: return "Fail field"
        }
        1 -> {
            isEqual(Demo("test1").foo(), "foo test1 update") ?: return "Fail function"
            isEqual(Demo("test2").foo_inline(), "inline foo test2") ?: return "Fail inline function"
            isEqual(Demo("test3").field1, "field1") ?: return "Fail field"
        }
        2, 3, 4 -> {
            isEqual(Demo("test1").foo(), "foo test1 update") ?: return "Fail function"
            isEqual(Demo("test2").foo_inline(), "inline foo test2 update") ?: return "Fail inline function"
            isEqual(Demo("test3").field1, "field1") ?: return "Fail field"
        }
        5, 6, 7, 8, 9 -> {
            isEqual(Demo("test1").foo(), "foo test1 update") ?: return "Fail function"
            isEqual(Demo("test2").foo_inline(), "inline foo test2 update") ?: return "Fail inline function"
            isEqual(Demo("test3").field1, "field1 update") ?: return "Fail field"
        }
        10 -> {
            isEqual(Demo("test1").foo(), "foo test1 update") ?: return "Fail function"
            isEqual(Demo("test2").foo_inline(), "inline foo test2 update") ?: return "Fail inline function"
            isEqual(Demo("test3").field1, 77) ?: return "Fail field"
        }
        11 -> {
            isEqual(Demo("test1").foo(), "foo test1 update") ?: return "Fail function"
            isEqual(Demo("test2").foo_inline(), "inline foo test2 update") ?: return "Fail inline function"
            isEqual(Demo("test3").field1, null) ?: return "Fail field"
        }
        else -> return "Unkown"
    }
    return "OK"
}
