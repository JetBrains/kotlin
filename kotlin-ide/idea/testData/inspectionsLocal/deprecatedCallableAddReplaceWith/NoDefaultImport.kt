// WITH_RUNTIME
<caret>@Deprecated("")
fun foo(s: String): String {
    return s.substring(1) + Int.MAX_VALUE
}
