package foo

external val c: Any? = noImpl

fun box(): String {
    if (c != null) return "fail1"
    return if (c == null) "OK" else "fail2"
}
