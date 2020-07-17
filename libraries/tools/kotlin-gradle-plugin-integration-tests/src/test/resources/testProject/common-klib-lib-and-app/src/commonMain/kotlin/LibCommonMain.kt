package com.h0tk3y.hmpp.klib.demo

interface LibCommonMainIface

expect class LibCommonMainExpect() : LibCommonMainIface {
    fun libCommonMainExpectFun(): Unit
}

fun libCommonMainTopLevelFun(): Int {
    println("commonMainTopLevelFun")
    return 2
}

fun libCommonMainInternalFun() = Unit

fun main() {
    
}

/** Test KT-37832 */
class MyCustomException : RuntimeException()