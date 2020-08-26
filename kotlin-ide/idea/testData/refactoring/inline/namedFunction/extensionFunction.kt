fun KotlinClass.functionFromKotlin(): Int = 42
class KotlinClass {
    fun <caret>a() = functionFromKotlin()
}

fun a() {
    KotlinClass().a()

    val d = KotlinClass()
    d.a()

    d.let {
        it.a()
    }

    d.also {
        it.a()
    }

    with(d) {
        a()
    }

    with(d) out@{
        with(4) {
            this@out.a()
        }
    }
}

fun a2() {
    val d: KotlinClass? = null
    d?.a()

    d?.let {
        it.a()
    }

    d?.also {
        it.a()
    }

    with(d) {
        this?.a()
    }

    with(d) out@{
        with(4) {
            this@out?.a()
        }
    }
}

fun a3() {
    val d: KotlinClass? = null
    val a1 = d?.a()

    val a2 = d?.let {
        it.a()
    }

    val a3 = d?.also {
        it.a()
    }

    val a4 = with(d) {
        this?.a()
    }

    val a5 = with(d) out@{
        with(4) {
            this@out?.a()
        }
    }
}

fun a4() {
    val d: KotlinClass? = null
    d?.a()?.dec()

    val a2 = d?.let {
        it.a()
    }
    a2?.toLong()

    d?.also {
        it.a()
    }?.a()?.and(4)

    val a4 = with(d) {
        this?.a()
    }

    val a5 = with(d) out@{
        with(4) {
            this@out?.a()
        }
    }

    val a6 = a4?.let { out -> a5?.let { out + it } }
}

fun KotlinClass.b(): Int? = a()
fun KotlinClass.c(): Int = this.a()
fun d(d: KotlinClass) = d.a()
