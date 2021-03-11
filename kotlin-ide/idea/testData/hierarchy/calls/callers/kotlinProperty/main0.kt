open class KBase {
    open var name = ""
}

class KA: KBase() {
    override var <caret>name = "A"
    fun foo(s: String): String = "A: $s"
}

fun packageFun(s: String): String = KBase().name + KA().name

val packageVal = KBase().name + KA().name

class KClient {
    init {
        KBase().name = ""
        KA().name = ""
    }

    companion object {
        val a = KBase().name + KA().name
    }

    var bar: String
        get() = KBase().name + KA().name
        set(value: String) {
            KBase().name = value
            KA().name = value
        }

    fun bar() {
        fun localFun() = KBase().name + KA().name

        val s = KBase().name + KA().name
    }
}

object KClientObj {
    val a = KBase().name + KA().name
    init {
        KBase().name = ""
        KA().name = ""
    }
}