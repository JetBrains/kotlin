package test

import apt.Anno
import generated.Test as TestGenerated
import generated.Property

@Anno
class Test {

    @field:Anno
    val property: String = ""

    @Anno
    fun function() {

    }

}

interface Usage {
    fun test(): TestGenerated
    fun test1(): generated.Function
    fun test2(): Property
}

fun main() {
    println("Generated class: " + TestGenerated::class.java.name)
}
