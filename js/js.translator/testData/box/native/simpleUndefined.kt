package foo

external interface MyAny
external val c: MyAny? = definedExternally

fun box(): String {
    if (c != null) return "fail1"
    return if (c == null) "OK" else "fail2"
}
