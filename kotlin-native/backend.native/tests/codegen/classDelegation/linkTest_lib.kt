/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package zzz

interface I {
    fun foo(): Int
}

open class A : I {
    override fun foo() = 42
}

open class B : I by A() {
    val x = 117
    val y = "zzz"
}