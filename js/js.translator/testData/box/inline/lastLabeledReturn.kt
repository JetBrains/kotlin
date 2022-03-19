inline fun foo(l: () -> Unit) { l() }
inline fun bar(l: () -> Unit) { l() }

fun box(): String {
    foo {
        bar {
            return@foo;
        }
        return "Failed: labeled return was not added"
    }
    return "OK"
}