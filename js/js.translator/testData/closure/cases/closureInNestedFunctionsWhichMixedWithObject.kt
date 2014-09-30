package foo

fun box(): Boolean {
    val t = run {
        object {
            fun boo(param: String): String {
                return run { param }
            }
        }
    }

    return t.boo("OK") == "OK"
}
