package test

import apt.Annotation1
import generated.Test123

@Annotation1
class Test

fun main() {
    println("Generated class: " + Test123::class.java.name)
}