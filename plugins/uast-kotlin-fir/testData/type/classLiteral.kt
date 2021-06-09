class Foo(val s: String) {
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other::class != this::class) return false
        return s == (other as Foo).s
    }
}

internal val stringClass = String::class

fun box(): String {
    val x: CharSequence = ""
    val xClass = x::class
    return if (xClass == stringClass) "OK" else "$xClass"
}
