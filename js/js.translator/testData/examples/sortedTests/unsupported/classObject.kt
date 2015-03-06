class C() {
    default object {
        fun create() = C()
    }
}

fun box(): String {
    val c = C.create()
    return if (c is C) "OK" else "fail"
}

