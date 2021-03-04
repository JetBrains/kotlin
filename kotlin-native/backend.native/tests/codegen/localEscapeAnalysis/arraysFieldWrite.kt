/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

class ArraysConstructor {
    private val memberArray: IntArray
    constructor(int1: Int, int2: Int) {
        memberArray = IntArray(2)
        set(int1, int2)
    }
    fun set(int1: Int, int2: Int) {
        memberArray[0] = int1
        memberArray[1] = int2
    }
    fun log() {
        println("Array (constructor init):")
        println("Size: ${memberArray.size}")
        println("Contents: ${memberArray.contentToString()}")
    }
}

class ArraysDefault {
    private val memberArray = IntArray(2)
    constructor(int1: Int, int2: Int) {
        set(int1, int2)
    }
    fun set(int1: Int, int2: Int) {
        memberArray[0] = int1
        memberArray[1] = int2
    }
    fun log() {
        println("Array (default value init):")
        println("Size: ${memberArray.size}")
        println("Contents: ${memberArray.contentToString()}")
    }
}

class ArraysInitBlock {
    private val memberArray : IntArray
    init {
        memberArray = IntArray(2)
    }
    constructor(int1: Int, int2: Int) {
        set(int1, int2)
    }
    fun set(int1: Int, int2: Int) {
        memberArray[0] = int1
        memberArray[1] = int2
    }
    fun log() {
        println("Array (init block):")
        println("Size: ${memberArray.size}")
        println("Contents: ${memberArray.contentToString()}")
    }
}

fun main() {
    val array1 = (::ArraysConstructor)(1, 2)
    array1.log()
    array1.set( 3, 4)
    array1.log()

    val array2 = (::ArraysDefault)(1, 2)
    array2.log()
    array2.set( 3, 4)
    array2.log()

    val array3 = (::ArraysInitBlock)(1, 2)
    array3.log()
    array3.set( 3, 4)
    array3.log()
}