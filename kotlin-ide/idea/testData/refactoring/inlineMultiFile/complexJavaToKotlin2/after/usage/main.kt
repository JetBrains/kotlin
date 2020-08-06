package usage

import javapackage.one.JavaClassOne
import javapackage.one.JavaClassOne.MAGIC_CONST
import javapackage.one.JavaClassOne.build

class KotlinClassOne {
    fun update(javaClassOne: JavaClassOne) {

    }
}

class KotlinClassTwo

fun usage() {
    val javaClass = JavaClassOne()
    // expect "val two = KotlinClassTwo()" after kotlinClassOne, KT-40867
    val kotlinClassOne = KotlinClassOne()
    val kotlinOther = KotlinClassOne()
    kotlinClassOne.update(javaClass)
    val result = build(kotlinOther.hashCode())
    kotlinOther.update(result)
    println(KotlinClassTwo())
    println(kotlinClassOne)
    System.err.println(result)
    build(result.convertToInt() + javaClass.hashCode() + javaClass.convertToInt() + MAGIC_CONST + build(javaClass.field).field)
}
val kotlinOne = KotlinClassOne()
val kotlinTwo = KotlinClassTwo()

fun a() {
    val javaClassOne = JavaClassOne()
    val kotlinOther = KotlinClassOne()
    kotlinOne.update(javaClassOne)
    val result = build(kotlinOther.hashCode())
    kotlinOther.update(result)
    println(kotlinTwo)
    println(kotlinOne)
    System.err.println(result)
    build(
        result.convertToInt() + javaClassOne.hashCode() + javaClassOne.convertToInt() + MAGIC_CONST + build(
            javaClassOne.field
        ).field
    )

    val d = JavaClassOne()
    val kotlinOther = KotlinClassOne()
    kotlinOne.update(d)
    val result = build(kotlinOther.hashCode())
    kotlinOther.update(result)
    println(kotlinTwo)
    println(kotlinOne)
    System.err.println(result)
    build(result.convertToInt() + d.hashCode() + d.convertToInt() + MAGIC_CONST + build(d.field).field)

    d.let {
        val kotlinOther = KotlinClassOne()
        kotlinOne.update(it)
        val result = build(kotlinOther.hashCode())
        kotlinOther.update(result)
        println(kotlinTwo)
        println(kotlinOne)
        System.err.println(result)
        build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
    }

    d.also {
        val kotlinOther = KotlinClassOne()
        kotlinOne.update(it)
        val result = build(kotlinOther.hashCode())
        kotlinOther.update(result)
        println(kotlinTwo)
        println(kotlinOne)
        System.err.println(result)
        build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
    }

    with(d) {
        val kotlinOther = KotlinClassOne()
        kotlinOne.update(this)
        val result = build(kotlinOther.hashCode())
        kotlinOther.update(result)
        println(kotlinTwo)
        println(kotlinOne)
        System.err.println(result)
        build(result.convertToInt() + hashCode() + convertToInt() + MAGIC_CONST + build(field).field)
    }

    with(d) out@{
        with(4) {
            val kotlinOther = KotlinClassOne()
            kotlinOne.update(this@out)
            val result = build(kotlinOther.hashCode())
            kotlinOther.update(result)
            println(kotlinTwo)
            println(kotlinOne)
            System.err.println(result)
            build(result.convertToInt() + this@out.hashCode() + this@out.convertToInt() + MAGIC_CONST + build(this@out.field).field)
        }
    }
}

fun a2() {
    val d: JavaClassOne? = null
    if (d != null) {
        val kotlinOther = KotlinClassOne()
        kotlinOne.update(d)
        val result = build(kotlinOther.hashCode())
        kotlinOther.update(result)
        println(kotlinTwo)
        println(kotlinOne)
        System.err.println(result)
        build(result.convertToInt() + d.hashCode() + d.convertToInt() + MAGIC_CONST + build(d.field).field)
    }

    d?.let {
        val kotlinOther = KotlinClassOne()
        kotlinOne.update(it)
        val result = build(kotlinOther.hashCode())
        kotlinOther.update(result)
        println(kotlinTwo)
        println(kotlinOne)
        System.err.println(result)
        build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
    }

    d?.also {
        val kotlinOther = KotlinClassOne()
        kotlinOne.update(it)
        val result = build(kotlinOther.hashCode())
        kotlinOther.update(result)
        println(kotlinTwo)
        println(kotlinOne)
        System.err.println(result)
        build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
    }

    with(d) {
        this?.let {
            val kotlinOther = KotlinClassOne()
            kotlinOne.update(it)
            val result = build(kotlinOther.hashCode())
            kotlinOther.update(result)
            println(kotlinTwo)
            println(kotlinOne)
            System.err.println(result)
            build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
        }
    }

    with(d) out@{
        with(4) {
            this@out?.let {
                val kotlinOther = KotlinClassOne()
                kotlinOne.update(it)
                val result = build(kotlinOther.hashCode())
                kotlinOther.update(result)
                println(kotlinTwo)
                println(kotlinOne)
                System.err.println(result)
                build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
            }
        }
    }
}

fun a3() {
    val d: JavaClassOne? = null
    val a1 = d?.let {
        val kotlinOther = KotlinClassOne()
        kotlinOne.update(it)
        val result = build(kotlinOther.hashCode())
        kotlinOther.update(result)
        println(kotlinTwo)
        println(kotlinOne)
        System.err.println(result)
        build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
    }

    val a2 = d?.let {
        val kotlinOther = KotlinClassOne()
        kotlinOne.update(it)
        val result = build(kotlinOther.hashCode())
        kotlinOther.update(result)
        println(kotlinTwo)
        println(kotlinOne)
        System.err.println(result)
        build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
    }

    val a3 = d?.also {
        val kotlinOther = KotlinClassOne()
        kotlinOne.update(it)
        val result = build(kotlinOther.hashCode())
        kotlinOther.update(result)
        println(kotlinTwo)
        println(kotlinOne)
        System.err.println(result)
        build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
    }

    val a4 = with(d) {
        this?.let {
            val kotlinOther = KotlinClassOne()
            kotlinOne.update(it)
            val result = build(kotlinOther.hashCode())
            kotlinOther.update(result)
            println(kotlinTwo)
            println(kotlinOne)
            System.err.println(result)
            build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
        }
    }

    val a5 = with(d) out@{
        with(4) {
            this@out?.let {
                val kotlinOther = KotlinClassOne()
                kotlinOne.update(it)
                val result = build(kotlinOther.hashCode())
                kotlinOther.update(result)
                println(kotlinTwo)
                println(kotlinOne)
                System.err.println(result)
                build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
            }
        }
    }
}

fun a4() {
    val d: JavaClassOne? = null
    d?.let {
        val kotlinOther = KotlinClassOne()
        kotlinOne.update(it)
        val result = build(kotlinOther.hashCode())
        kotlinOther.update(result)
        println(kotlinTwo)
        println(kotlinOne)
        System.err.println(result)
        build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
    }?.convertToInt()?.dec()

    val a2 = d?.let {
        val kotlinOther = KotlinClassOne()
        kotlinOne.update(it)
        val result = build(kotlinOther.hashCode())
        kotlinOther.update(result)
        println(kotlinTwo)
        println(kotlinOne)
        System.err.println(result)
        build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
    }
    a2?.convertToInt()?.toLong()

    val also = d?.also {
        it.apply(kotlinOne, kotlinTwo)
    }
    if (also != null) {
        val kotlinOther = KotlinClassOne()
        kotlinOne.update(also)
        val result = build(kotlinOther.hashCode())
        kotlinOther.update(result)
        println(kotlinTwo)
        println(kotlinOne)
        System.err.println(result)
        build(result.convertToInt() + also.hashCode() + also.convertToInt() + MAGIC_CONST + build(also.field).field)
    }

    val a4 = with(d) {
        this?.let {
            val kotlinOther = KotlinClassOne()
            kotlinOne.update(it)
            val result = build(kotlinOther.hashCode())
            kotlinOther.update(result)
            println(kotlinTwo)
            println(kotlinOne)
            System.err.println(result)
            build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
        }
    }?.convertToInt()

    val a5 = with(d) out@{
        with(4) {
            this@out?.let {
                val kotlinOther = KotlinClassOne()
                kotlinOne.update(it)
                val result = build(kotlinOther.hashCode())
                kotlinOther.update(result)
                println(kotlinTwo)
                println(kotlinOne)
                System.err.println(result)
                build(result.convertToInt() + it.hashCode() + it.convertToInt() + MAGIC_CONST + build(it.field).field)
            }
        }
    }?.convertToInt()

    val a6 = a4?.let { out -> a5?.let { out + it } }
}

fun JavaClassOne.b(): Int? {
    val kotlinOther = KotlinClassOne()
    kotlinOne.update(this)
    val result = build(kotlinOther.hashCode())
    kotlinOther.update(result)
    println(kotlinTwo)
    println(kotlinOne)
    System.err.println(result)
    return build(result.convertToInt() + hashCode() + convertToInt() + MAGIC_CONST + build(field).field).convertToInt()
}
fun JavaClassOne.c(): Int {
    val kotlinOther = KotlinClassOne()
    kotlinOne.update(this)
    val result = build(kotlinOther.hashCode())
    kotlinOther.update(result)
    println(kotlinTwo)
    println(kotlinOne)
    System.err.println(result)
    return build(result.convertToInt() + this.hashCode() + this.convertToInt() + MAGIC_CONST + build(this.field).field).convertToInt()
}
fun d(d: JavaClassOne): Int {
    val kotlinOther = KotlinClassOne()
    kotlinOne.update(d)
    val result = build(kotlinOther.hashCode())
    kotlinOther.update(result)
    println(kotlinTwo)
    println(kotlinOne)
    System.err.println(result)
    return build(result.convertToInt() + d.hashCode() + d.convertToInt() + MAGIC_CONST + build(d.field).field).convertToInt()
}
