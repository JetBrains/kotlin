package test

class Class {
    fun method(): Int {
        val square: (Int) -> Int = { it * it }
        return square(2)
    }
}