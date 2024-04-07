/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

private inline fun <T> inlineFun(block: () -> T): T {
    val zalue = block()
    return zalue
}

interface Bar {
    fun bar()
}

interface Foo {
    fun getBar(): Bar
}

val foo = inlineFun {
    object : Foo {
        override fun getBar(): Bar {
            return inlineFun {
                object : Bar {
                    override fun bar() { }
                }
            }
        }
    }
}

fun callbar(bar: Bar) = bar.bar()
