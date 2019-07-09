package test

import java.util.Random

object Test {
    fun x() {
        val a: Int
        val b: Int
        when (Random().nextInt()) {
            0 -> {
                b = 1
                a = b
            }
            else -> {
                b = -1
                a = b
            }
        }
        println(a + b)
    }
}
