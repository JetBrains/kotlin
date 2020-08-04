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

fun JavaClass.b() = a()
fun JavaClass.c() = this.a()
fun d(d: JavaClass) = d.a()
