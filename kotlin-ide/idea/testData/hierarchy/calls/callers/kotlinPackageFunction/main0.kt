fun <caret>packageFun(s: String): String = s

val packageVal = packageFun("")

class KClient {
    init {
        packageFun("")
    }

    companion object {
        val a = packageFun("")
    }

    val bar: String
        get() = packageFun("")

    fun bar() {
        fun localFun() = packageFun("")

        packageFun("")
    }
}

object KClientObj {
    val a = packageFun("")
}