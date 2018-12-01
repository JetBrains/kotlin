package test

class Class {
    val property: Int = 0
}

fun getProp(c: Class) = Class::property.get(c)