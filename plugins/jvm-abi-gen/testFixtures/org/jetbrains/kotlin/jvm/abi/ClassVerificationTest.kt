/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import java.net.URLClassLoader

class ClassVerificationTest : BaseJvmAbiTest() {
    fun testSimple() {
        workingDir.resolve("test.kt").writeText(
            """
            package test
            
            class C(val x: String) {
                constructor(y: Int = 0) : this(y.toString())
                fun f(z: String): String = x + z
                var q: String = x
                inline fun i(u: Int = 1): Int = x.toInt() + u
            }
            
            typealias D = Double
            fun g(v: Number): Int = v.toInt()
            var h: D = 3.14
        """.trimIndent()
        )
        val compilation = Compilation(workingDir, name = null).also { make(it) }

        val classLoader = URLClassLoader(arrayOf(compilation.abiDir.toURI().toURL()), null)
        classLoader.loadClass("test.C")
        classLoader.loadClass("test.TestKt")
    }
}
