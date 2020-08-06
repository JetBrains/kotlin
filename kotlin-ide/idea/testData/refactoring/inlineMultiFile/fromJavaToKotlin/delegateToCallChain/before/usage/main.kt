package usage

import javapackage.one.JavaClassOne

/*
    missing import "kotlinpackage.two.extensionSelf"
    KT-40856
 */
fun a() {
    JavaClassOne().<caret>a()

    val d = JavaClassOne()
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
    val d: JavaClassOne? = null
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
    val d: JavaClassOne? = null
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
    val d: JavaClassOne? = null
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

fun JavaClassOne.b(): Int? = a()
fun JavaClassOne.c(): Int = this.a()
fun d(d: JavaClassOne) = d.a()
