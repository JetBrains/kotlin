/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

open class Foo(val z: Int) {
    open inner class FooInner {
        fun foo() = z
    }
}