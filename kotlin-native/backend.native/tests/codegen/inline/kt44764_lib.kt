/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

interface Foo {
    fun f(s: String): String
}

inline fun foo(crossinline block: (String) -> String) = object : Foo {
    override fun toString() = zzz { "zzz" }
    override fun f(s: String) = block(s)
    private inline fun zzz(z: () -> String) = z()
}

inline fun bar(crossinline block: (String) -> String) = foo(block)
