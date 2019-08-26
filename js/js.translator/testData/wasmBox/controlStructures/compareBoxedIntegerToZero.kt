fun box(): String {
    val x: Int = 0
    if (x != 0) return "Fail"
    if (0 != x) return "Fail"
    if (!(x == 0)) return "Fail"
    if (!(0 == x)) return "Fail"
    return "OK"
}
