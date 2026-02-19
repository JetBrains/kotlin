/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

fun thrower() : Nothing {
    throw Exception()
}

fun getStackTrace() : List<String> {
    try {
       thrower()
    }  catch (e: Exception) {
        return e.getStackTrace().toList()
    }
    return emptyList()
}

open class Foo {
    open fun foo(): List<String> { return emptyList() }
}

private class Bar : Foo() {
    override fun foo(): List<String> = getStackTrace()
}

object Object {
    var trace: List<String> = emptyList()

    init {
        trace = getStackTrace()
    }
}

class WithCompanion {
    companion object {
        var trace: List<String> = emptyList()

        init {
            trace = getStackTrace()
        }
    }
}

enum class E {
    A {
        init {
            trace = getStackTrace()
        }
    };

    var trace: List<String> = emptyList()
}

fun createBar(): Foo = Bar()

fun use(foo: Foo): List<String> {
    return foo.foo()
}