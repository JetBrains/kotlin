/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package cases.annotations

annotation class Foo(val i: Int)

private class Bar {
    val foo: Foo = Foo(1) // Same module
    val e = Volatile() // Cross-module

    fun bar() {
        foo()
    }
}
