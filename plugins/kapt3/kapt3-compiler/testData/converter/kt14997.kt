@file:Suppress("AMBIGUOUS_ANONYMOUS_TYPE_INFERRED")

open class CrashMe {
    private val notReally = object : Runnable {
        override fun run() {
            throw UnsupportedOperationException()
        }
    }
}

fun a() = object : Runnable {
    override fun run() {}
}

fun b() = object : java.io.Serializable, Runnable {
    override fun run() {}
}

fun c() = object : CrashMe(), Runnable {
    override fun run() {}
}