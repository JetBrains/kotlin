package test

import apt.Anno
import generated.Example as ExampleGenerated

@Anno
class Example() {
    private val callback = object : Any() {
        val obj = Object()
    }

    fun call() {
        callback.obj
    }
}

fun main() {
    println("Generated class: " + ExampleGenerated::class.java.name)
}