/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// All classes and methods should be used in tests
@file:Suppress("UNUSED")

package localEA

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
    fun log() = "size: ${memberArray.size}, contents: ${memberArray.contentToString()}"
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
    fun log() = "size: ${memberArray.size}, contents: ${memberArray.contentToString()}"
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
    fun log() = "size: ${memberArray.size}, contents: ${memberArray.contentToString()}"
}

