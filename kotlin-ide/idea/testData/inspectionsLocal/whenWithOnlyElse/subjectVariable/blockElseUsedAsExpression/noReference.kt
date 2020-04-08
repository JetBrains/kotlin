// WITH_RUNTIME
fun test() {
    val x = when<caret> (val a = create()) {
        else -> {
            use("")
        }
    }
}

fun create(): String = ""

fun use(s: String) {}