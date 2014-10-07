package foo

fun box(): String {
    val t = run {
                object {
                    fun foo() = "3"

                    fun boo(param: String): String {
                        val a = object {
                            fun bar() = "57"
                            fun b(): String = run { param + bar() + foo() }
                        }

                        return a.b()
                    }
                }
            }

    val r = t.boo("OK")
    if (r != "OK573") return "r != \"OK573\", r = \"$r\""

    return "OK"
}
