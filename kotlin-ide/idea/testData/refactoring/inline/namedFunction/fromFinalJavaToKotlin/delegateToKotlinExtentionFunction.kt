fun JavaClass.functionFromKotlin(): Int = 42

fun a() {
    JavaClass().a()

    val d = JavaClass()
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
    val d: JavaClass? = null
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
    val d: JavaClass? = null
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
    val d: JavaClass? = null
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

fun JavaClass.b(): Int? = a()
fun JavaClass.c(): Int = this.a()
fun d(d: JavaClass) = d.a()
