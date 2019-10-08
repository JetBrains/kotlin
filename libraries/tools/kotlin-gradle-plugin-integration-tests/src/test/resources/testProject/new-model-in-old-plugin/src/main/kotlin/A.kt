package com.example

class A {
    fun f(): String = "hello".also(::println)
}

fun internalFun() = 1