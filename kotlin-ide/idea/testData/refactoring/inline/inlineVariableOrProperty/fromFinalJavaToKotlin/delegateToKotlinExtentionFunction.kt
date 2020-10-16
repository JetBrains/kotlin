fun JavaClass.functionFromKotlin(): Int = 42

fun a() {
    JavaClass().field

    val d = JavaClass()
    d.field

    d.let {
        it.field
    }

    d.also {
        it.field
    }

    with(d) {
        field
    }

    with(d) out@{
        with(4) {
            this@out.field
        }
    }
}

fun a2() {
    val d: JavaClass? = null
    d?.field

    d?.let {
        it.field
    }

    d?.also {
        it.field
    }

    with(d) {
        this?.field
    }

    with(d) out@{
        with(4) {
            this@out?.field
        }
    }
}

fun a3() {
    val d: JavaClass? = null
    val a1 = d?.field

    val a2 = d?.let {
        it.field
    }

    val a3 = d?.also {
        it.field
    }

    val a4 = with(d) {
        this?.field
    }

    val a5 = with(d) out@{
        with(4) {
            this@out?.field
        }
    }
}

fun a4() {
    val d: JavaClass? = null
    d?.field?.dec()

    val a2 = d?.let {
        it.field
    }
    a2?.toLong()

    d?.also {
        it.field
    }?.field?.and(4)

    val a4 = with(d) {
        this?.field
    }

    val a5 = with(d) out@{
        with(4) {
            this@out?.field
        }
    }

    val a6 = a4?.let { out -> a5?.let { out + it } }
}

fun JavaClass.b(): Int? = field
fun JavaClass.c(): Int = this.field
fun d(d: JavaClass) = d.field
