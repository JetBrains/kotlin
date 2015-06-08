package foo

fun box(): Boolean {
    val t = myRun {
        object {
            fun boo(param: String): String {
                return myRun { param }
            }
        }
    }

    return t.boo("OK") == "OK"
}
