package usage

import javapackage.one.JavaClassOne
import kotlinpackage.one.extensionSelf

/*
    missing import "kotlinpackage.two.extensionSelf"
    KT-40856
 */
fun a() {
    val javaClassOne = JavaClassOne()
    println(javaClassOne.onlySuperMethod())
    println(javaClassOne.superClassMethod())
    println(javaClassOne.superClassMethod())
    JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()

    val d = JavaClassOne()
    println(d.onlySuperMethod())
    println(d.superClassMethod())
    println(d.superClassMethod())
    JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()

    d.let {
        println(it.onlySuperMethod())
        println(it.superClassMethod())
        println(it.superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    d.also {
        println(it.onlySuperMethod())
        println(it.superClassMethod())
        println(it.superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    with(d) {
        println(onlySuperMethod())
        println(superClassMethod())
        println(superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    with(d) out@{
        with(4) {
            println(this@out.onlySuperMethod())
            println(this@out.superClassMethod())
            println(this@out.superClassMethod())
            JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
        }
    }
}

fun a2() {
    val d: JavaClassOne? = null
    if (d != null) {
        println(d.onlySuperMethod())
        println(d.superClassMethod())
        println(d.superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    d?.let {
        println(it.onlySuperMethod())
        println(it.superClassMethod())
        println(it.superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    d?.also {
        println(it.onlySuperMethod())
        println(it.superClassMethod())
        println(it.superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    with(d) {
        this?.let {
            println(it.onlySuperMethod())
            println(it.superClassMethod())
            println(it.superClassMethod())
            JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
        }
    }

    with(d) out@{
        with(4) {
            this@out?.let {
                println(it.onlySuperMethod())
                println(it.superClassMethod())
                println(it.superClassMethod())
                JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
            }
        }
    }
}

fun a3() {
    val d: JavaClassOne? = null
    val a1 = d?.let {
        println(it.onlySuperMethod())
        println(it.superClassMethod())
        println(it.superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    val a2 = d?.let {
        println(it.onlySuperMethod())
        println(it.superClassMethod())
        println(it.superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    val a3 = d?.also {
        println(it.onlySuperMethod())
        println(it.superClassMethod())
        println(it.superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }

    val a4 = with(d) {
        this?.let {
            println(it.onlySuperMethod())
            println(it.superClassMethod())
            println(it.superClassMethod())
            JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
        }
    }

    val a5 = with(d) out@{
        with(4) {
            this@out?.let {
                println(it.onlySuperMethod())
                println(it.superClassMethod())
                println(it.superClassMethod())
                JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
            }
        }
    }
}

fun a4() {
    val d: JavaClassOne? = null
    d?.let {
        println(it.onlySuperMethod())
        println(it.superClassMethod())
        println(it.superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }?.dec()

    val a2 = d?.let {
        println(it.onlySuperMethod())
        println(it.superClassMethod())
        println(it.superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }
    a2?.toLong()

    d?.also {
        println(it.onlySuperMethod())
        println(it.superClassMethod())
        println(it.superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }?.let {
        println(it.onlySuperMethod())
        println(it.superClassMethod())
        println(it.superClassMethod())
        JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
    }?.and(4)

    val a4 = with(d) {
        this?.let {
            println(it.onlySuperMethod())
            println(it.superClassMethod())
            println(it.superClassMethod())
            JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
        }
    }

    val a5 = with(d) out@{
        with(4) {
            this@out?.let {
                println(it.onlySuperMethod())
                println(it.superClassMethod())
                println(it.superClassMethod())
                JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
            }
        }
    }

    val a6 = a4?.let { out -> a5?.let { out + it } }
}

fun JavaClassOne.b(): Int? {
    println(onlySuperMethod())
    println(superClassMethod())
    println(superClassMethod())
    return JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
}
fun JavaClassOne.c(): Int {
    println(this.onlySuperMethod())
    println(this.superClassMethod())
    println(this.superClassMethod())
    return JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
}
fun d(d: JavaClassOne): Int? {
    println(d.onlySuperMethod())
    println(d.superClassMethod())
    println(d.superClassMethod())
    return JavaClassOne().extensionSelf().toJavaClassTwo().extensionSelf().returnSelf().toJavaOne().otherMethod()
}
