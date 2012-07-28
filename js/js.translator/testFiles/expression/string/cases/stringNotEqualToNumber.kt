package foo


fun box(): Boolean {
    val t1: Any = "3"
    val t2: Any = 3
    val t3: Any = "4"
    val t4: Any = 4
    if (t3 == t4) return false
    return t1 != t2
}