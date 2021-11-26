/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

class Bar : Foo(42) {
    inner class BarInner(val x: Int) : FooInner()
}

fun main() {
    val o = Bar().BarInner(117)
    println(o.x)
    println(o.foo())
}