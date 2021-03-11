package usage

import javapackage.one.JavaClassOne

class KotlinClassOne {
    fun update(javaClassOne: JavaClassOne) {

    }
}

class KotlinClassTwo

fun usage() {
    val javaClass = JavaClassOne()
    // expect "val two = KotlinClassTwo()" after kotlinClassOne, KT-40867
    javaClass.app<caret>ly(KotlinClassOne(), KotlinClassTwo())
}
val kotlinOne = KotlinClassOne()
val kotlinTwo = KotlinClassTwo()

fun a() {
    JavaClassOne().apply(kotlinOne, kotlinTwo)

    val d = JavaClassOne()
    d.apply(kotlinOne, kotlinTwo)

    d.let {
        it.apply(kotlinOne, kotlinTwo)
    }

    d.also {
        it.apply(kotlinOne, kotlinTwo)
    }

    with(d) {
        apply(kotlinOne, kotlinTwo)
    }

    with(d) out@{
        with(4) {
            this@out.apply(kotlinOne, kotlinTwo)
        }
    }
}

fun a2() {
    val d: JavaClassOne? = null
    d?.apply(kotlinOne, kotlinTwo)

    d?.let {
        it.apply(kotlinOne, kotlinTwo)
    }

    d?.also {
        it.apply(kotlinOne, kotlinTwo)
    }

    with(d) {
        this?.apply(kotlinOne, kotlinTwo)
    }

    with(d) out@{
        with(4) {
            this@out?.apply(kotlinOne, kotlinTwo)
        }
    }
}

fun a3() {
    val d: JavaClassOne? = null
    val a1 = d?.apply(kotlinOne, kotlinTwo)

    val a2 = d?.let {
        it.apply(kotlinOne, kotlinTwo)
    }

    val a3 = d?.also {
        it.apply(kotlinOne, kotlinTwo)
    }

    val a4 = with(d) {
        this?.apply(kotlinOne, kotlinTwo)
    }

    val a5 = with(d) out@{
        with(4) {
            this@out?.apply(kotlinOne, kotlinTwo)
        }
    }
}

fun a4() {
    val d: JavaClassOne? = null
    d?.apply(kotlinOne, kotlinTwo)?.convertToInt()?.dec()

    val a2 = d?.let {
        it.apply(kotlinOne, kotlinTwo)
    }
    a2?.convertToInt()?.toLong()

    d?.also {
        it.apply(kotlinOne, kotlinTwo)
    }?.apply(kotlinOne, kotlinTwo)

    val a4 = with(d) {
        this?.apply(kotlinOne, kotlinTwo)
    }?.convertToInt()

    val a5 = with(d) out@{
        with(4) {
            this@out?.apply(kotlinOne, kotlinTwo)
        }
    }?.convertToInt()

    val a6 = a4?.let { out -> a5?.let { out + it } }
}

fun JavaClassOne.b(): Int? = apply(kotlinOne, kotlinTwo).convertToInt()
fun JavaClassOne.c(): Int = this.apply(kotlinOne, kotlinTwo).convertToInt()
fun d(d: JavaClassOne) = d.apply(kotlinOne, kotlinTwo).convertToInt()
