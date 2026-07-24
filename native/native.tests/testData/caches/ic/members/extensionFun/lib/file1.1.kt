package test

class User(val age: Int)

fun User.foo(): String = "Extension function v2: ${this.age}"
