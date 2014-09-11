package foo

native
val c: Any? = js.noImpl

fun box(): Boolean {
    if (c != null) return false
    return (c == null)
}
