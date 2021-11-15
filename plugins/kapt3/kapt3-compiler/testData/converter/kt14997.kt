// WITH_STDLIB
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

fun d() = listOf(object : Runnable {
    override fun run() {}
})

fun e() = arrayOf(object : Runnable {
    override fun run() {}
})

fun e1(a: Array<CharSequence>) {}
fun e2(a: Array<in CharSequence>) {}
fun e3(a: Array<out CharSequence>) {}
fun e3(a: Array<*>) {}