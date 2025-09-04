inline fun callIt(f: () -> Int) = f()

fun box(stepId: Int, isWasm: Boolean): String {
    val a = object : ClassA() {
        override fun test1() = "object::test1".castTo<String>()
        override fun test2() = 2.castTo<String>()
    }

    val b = ClassB()

    if (a.test1() != "object::test1") return "Fail 1"
    if (b.test1() != "ClassB::test1") return "Fail 2"

    when (stepId) {
        0 -> {
            if (a.test2() != null) return "Fail 0-1"
            if (b.test2() != null) return "Fail 0-2"

            if (a.fakeOverrideFunction() != 0) return "Fail 0-3"
            if (b.fakeOverrideFunction() != 0) return "Fail 0-4"

            if (callIt(a::fakeOverrideFunction) != 0) return "Fail 0-5"
            if (callIt(b::fakeOverrideFunction) != 0) return "Fail 0-6"
        }
        1 -> {
            if (a.test2() != "OTHER 2") return "Fail 1-1"
            if (b.test2() != "OTHER 1") return "Fail 1-2"
        }
        2 -> {
            if (a.fakeOverrideFunction() != 2) return "Fail 2-1"
            if (b.fakeOverrideFunction() != 2) return "Fail 2-2"

            if (callIt(a::fakeOverrideFunction) != 2) return "Fail 2-3"
            if (callIt(b::fakeOverrideFunction) != 2) return "Fail 2-4"
        }
        else -> return "Unknown"
    }
    return "OK"
}
