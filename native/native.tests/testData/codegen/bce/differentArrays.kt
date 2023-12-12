/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

class Foo(size: Int) {
    val array = IntArray(size)
}

class Bar {
    val smallFoo = Foo(1)
    val largeFoo = Foo(10)

    val smallArray = smallFoo.array
    val largeArray = largeFoo.array
}

fun box(): String {
    val bar = Bar()

    assertFailsWith<IndexOutOfBoundsException> {
        for (index in 0 until bar.largeArray.size) {
            bar.smallArray[index] = 6
        }
    }
    return "OK"
}