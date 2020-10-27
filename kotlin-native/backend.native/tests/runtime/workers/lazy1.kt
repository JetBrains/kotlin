/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.lazy1

import kotlin.test.*
import kotlin.native.concurrent.*

class Lazy {
    val x = 17
    val self by lazy { this }
    val recursion: Int by lazy {
        if (x < 17) 42 else recursion
    }
    val freezer: Int by lazy {
        freeze()
        42
    }
    val thrower: String by lazy {
        if (x < 100) throw IllegalArgumentException()
        "FAIL"
    }
}

@Test fun runTest1() {
    assertFailsWith<IllegalStateException> {
        println(Lazy().recursion)
    }
    assertFailsWith<IllegalStateException> {
        println(Lazy().freeze().recursion)
    }
}

@Test fun runTest2() {
    var sum = 0
    for (i in 1 .. 100) {
        val self = Lazy().freeze()
        assertEquals(self, self.self)
        sum += self.self.hashCode()
    }
    println("OK")
}


@Test fun runTest3() {
    assertFailsWith<InvalidMutabilityException> {
        println(Lazy().freezer)
    }
}

@Test fun runTest4() {
    val self = Lazy()
    repeat(10) {
        assertFailsWith<IllegalArgumentException> {
            println(self.thrower)
        }
    }
}