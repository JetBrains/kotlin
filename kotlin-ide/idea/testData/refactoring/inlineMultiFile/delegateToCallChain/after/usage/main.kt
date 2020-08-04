package usage

import javapackage.one.JavaClassOne
import kotlinpackage.one.extensionSelf
import kotlinpackage.two.extensionSelf

fun a() {
    JavaClassOne()
    JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()

    val d = JavaClassOne()
    JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()

    d.let {
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    d.also {
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    with(d) {
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    with(d) out@{
        with(4) {
            JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
        }
    }
}

fun a2() {
    val d: JavaClassOne? = null
    d?.let { JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod() }

    d?.let {
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    d?.also {
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    with(d) {
        this?.let { JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod() }
    }

    with(d) out@{
        with(4) {
            this@out?.let { JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod() }
        }
    }
}

fun a3() {
    val d: JavaClassOne? = null
    val a1 = d?.let { JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod() }

    val a2 = d?.let {
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    val a3 = d?.also {
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    val a4 = with(d) {
        this?.let { JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod() }
    }

    val a5 = with(d) out@{
        with(4) {
            this@out?.let { JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod() }
        }
    }
}

fun a4() {
    val d: JavaClassOne? = null
    d?.let { JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod() }?.dec()

    val a2 = d?.let {
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }
    a2?.toLong()

    d?.also {
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }?.let { JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod() }?.and(4)

    val a4 = with(d) {
        this?.let { JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod() }
    }

    val a5 = with(d) out@{
        with(4) {
            this@out?.let { JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod() }
        }
    }

    val a6 = a4?.let { out -> a5?.let { out + it } }
}

fun JavaClassOne.b(): Int? = JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
fun JavaClassOne.c(): Int = JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
fun d(d: JavaClassOne) = JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
