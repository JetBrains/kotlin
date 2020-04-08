var <caret>packageVal = ""

fun packageFun(s: String): String = packageVal + s

class KClient {
    init {
        packageVal = ""
    }

    companion object {
        val a = packageVal
    }

    val bar: String
        get() = packageVal
        set(value: String) {packageVal = value}

    fun bar() {
        fun localFun() = packageVal

        val s = packageVal
    }
}

object KClientObj {
    val a = packageVal
    init {
        packageVal = ""
    }
}