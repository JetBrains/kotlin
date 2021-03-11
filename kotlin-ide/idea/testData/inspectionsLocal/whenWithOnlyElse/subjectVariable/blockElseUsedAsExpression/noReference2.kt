// WITH_RUNTIME
fun test() {
    val x = when<caret> (val a = 42) {
        else -> {
            use("")
        }
    }
}

fun use(s: String) {}