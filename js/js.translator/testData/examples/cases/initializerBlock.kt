class C() {
    public var f: Int

    {
        $f = 610
    }
}

fun box(): String {
    val c = C()
    if (c.f != 610) return "fail"
    return "OK"
}
