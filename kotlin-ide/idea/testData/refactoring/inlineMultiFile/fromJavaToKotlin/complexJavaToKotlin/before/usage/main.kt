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
