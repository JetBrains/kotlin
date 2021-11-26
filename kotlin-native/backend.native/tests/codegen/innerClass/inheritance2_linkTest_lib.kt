/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

open class Outer(val x: Int) {
    open inner class Inner1
    inner class Middle(x: Int) : Outer(x) {
        inner class Inner2 : Inner1() {
            fun foo() = this@Outer.x + this@Middle.x
        }
    }
}