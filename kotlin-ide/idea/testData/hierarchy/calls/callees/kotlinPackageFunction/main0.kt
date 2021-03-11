class KA {
    val name = "A"
    fun foo(s: String): String = "A: $s"
}

val packageVal = ""

fun <caret>packageFun() {
    fun localFun(s: String): String = s
    val localVal = localFun("")

    KA().foo(KA().name)
    JA().foo(JA().name)
    localFun(packageVal)

    run {
        KA().foo(KA().name)
        JA().foo(JA().name)
        KA().foo(localVal)
    }
}