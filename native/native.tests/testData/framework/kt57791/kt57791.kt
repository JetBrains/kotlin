/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

interface Foo {
    fun bar(): String?
}

internal class FooImplUnused : Foo {
    override fun bar(): String? = null
}

fun foobar(foo: Foo): Boolean {
    val s = foo.bar()
    if (s == null)
        return false
    return s == "zzz"
}