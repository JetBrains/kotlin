/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

class First(initArray: Array<Int>) {
    val array = initArray
}

class Second(initArray: Array<Int>){
    val first = First(initArray)
}

class Third(initArray: Array<Int>) {
    val second = Second(initArray)
}

fun box(): String {
    val a = Third(arrayOf(1, 2, 3, 4, 5))
    val b = Third(arrayOf(1, 2))

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0..a.second.first.array.size-1) {
            b.second.first.array[i] = 6
        }
    }
    return "OK"
}