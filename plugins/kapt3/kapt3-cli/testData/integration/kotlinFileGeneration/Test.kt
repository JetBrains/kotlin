package test

import apt.Anno
import generated.Test as TestGenerated

@Anno
class Test {
    @field:Anno
    val property: String = ""

    @Anno
    fun function() {

    }
}

fun main() {
    println("Generated class: " + TestGenerated::class.java.name)
}