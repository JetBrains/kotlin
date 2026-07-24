package test

class User(val age: Int)

fun User.foo(): String = "Extension function: ${this.age}"
