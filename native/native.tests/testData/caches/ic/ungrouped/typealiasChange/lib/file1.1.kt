package test

class Dog(val x: Int) {
    fun foo(): Int = x
}

class Cat(val x: Int) {
    fun foo(): Int = x + 100
}

typealias A = Cat
