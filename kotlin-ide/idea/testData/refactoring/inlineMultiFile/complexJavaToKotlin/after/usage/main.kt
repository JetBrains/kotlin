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
