/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.statements0

import kotlin.test.*

fun simple() {
    var a = 238
    a++
    println(a)
    --a
    println(a)
}

class Foo() {
    val j = 2
    var i = 29

    fun more() {
        i++
    }

    fun less() {
        --i
    }
}

fun fields() {
    val foo = Foo()
    foo.more()
    println(foo.i)
    foo.less()
    println(foo.i)
}

@Test fun runTest() {
    simple()
    fields()
}