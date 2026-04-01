package test

import kotlin.reflect.full.memberProperties

data class Cat(val name: String, val age: Int)

fun main() {
    val props = Cat::class.memberProperties
    println("Cat properties: ${props.map { it.name }}")
}