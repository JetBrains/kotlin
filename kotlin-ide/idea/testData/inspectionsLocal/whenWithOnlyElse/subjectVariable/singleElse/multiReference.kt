// WITH_RUNTIME
fun test() {
    when<caret> (val a = create()) {
        else -> use(a, a)
    }
}

fun create(): String = ""

fun use(s: String, t: String) {}