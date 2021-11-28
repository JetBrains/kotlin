/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

open class Outer {
    open inner class Inner1
    inner class Middle {
        inner class Inner2 : Inner1() {
            fun getOuter() = this@Outer
            fun getMiddle() = this@Middle
        }
    }
}