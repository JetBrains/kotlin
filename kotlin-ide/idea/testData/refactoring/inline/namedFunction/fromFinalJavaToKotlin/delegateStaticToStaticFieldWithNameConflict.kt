val staticField = 42 //KT-40835

fun a() {
    JavaClass.a()

    val d = JavaClass()
    JavaClass.a()

    d.let {
        JavaClass.a()
    }

    d.also {
        JavaClass.a()
    }

    with(d) {
        JavaClass.a()
    }

    with(d) out@{
        with(4) {
            JavaClass.a()
        }
    }
}

fun a2() {
    val d: JavaClass? = null
    d?.a()

    d?.let {
        JavaClass.a()
    }

    d?.also {
        JavaClass.a()
    }

    with(d) {
        JavaClass.a()
    }

    with(d) out@{
        with(4) {
            JavaClass.a()
        }
    }
}

fun JavaClass.b(): Int? = a()
fun JavaClass.c(): Int = JavaClass.a()
fun d(d: JavaClass) = JavaClass.a()
