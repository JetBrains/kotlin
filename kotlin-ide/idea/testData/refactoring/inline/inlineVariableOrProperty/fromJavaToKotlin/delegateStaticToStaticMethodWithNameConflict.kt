fun staticMethod() = Unit //KT-40835

fun a() {
    JavaClass.field

    val d = JavaClass()
    JavaClass.field

    d.let {
        JavaClass.field
    }

    d.also {
        JavaClass.field
    }

    with(d) {
        JavaClass.field
    }

    with(d) out@{
        with(4) {
            JavaClass.field
        }
    }
}

fun a2() {
    val d: JavaClass? = null
    d?.field

    d?.let {
        JavaClass.field
    }

    d?.also {
        JavaClass.field
    }

    with(d) {
        JavaClass.field
    }

    with(d) out@{
        with(4) {
            JavaClass.field
        }
    }
}

fun JavaClass.b(): Int? = JavaClass.field
fun JavaClass.c(): Int = JavaClass.field
fun d(d: JavaClass) = JavaClass.field
