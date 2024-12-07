fun box(stepId: Int, isWasm: Boolean): String {
    val s = test()
    when (stepId) {
        0 -> if (s != "class A - testFunction") return "Fail, got '$s'"
        1 -> if (s != "class B - testFunction") return "Fail, got '$s'"
        2 -> if (s != "class A - testFunction, class B - testFunction") return "Fail, got '$s'"
        3 -> if (s != "class A - testFunction, class B - testFunction, class C - testFunction") return "Fail, got '$s'"
        else -> return "Unknown"
    }
    return "OK"
}
