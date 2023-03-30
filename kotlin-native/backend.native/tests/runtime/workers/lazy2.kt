/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

object Foo {
    val bar = Bar()
}

class Bar {
    val f by lazy {
        foo()
    }

    fun foo() = 123
}

fun printAll() {
    println(Foo.bar.f)
}

// This test is extracted from the real problem found in kotlinx.serialization, where zeroing out
// initializer field in frozen lazy object led to the crash, induced by breaking frozen objects'
// invariant (initializer end up in the same container as the lazy object itself, so it was destroyed
// earlier than it should when reference counter was decremented).
@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
fun main(args: Array<String>) {
    printAll()
    kotlin.native.runtime.GC.collect()
    println("OK")
}