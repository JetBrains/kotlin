package com.example

@SomeAnnotation
object SomeClass {
    init {
        println(StringFactory.generateString())
    }
}

fun library() {
    GeneratedSomeClass
    println(StringFactory.generateString())
}