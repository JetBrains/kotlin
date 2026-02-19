/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import com.intellij.openapi.Disposable
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import java.io.File

/**
 * Test Environment that enables quick 'inline' creation of [ModuleDescriptor] instances.
 *
 * # Examples
 * ## Test operating on a single Kotlin Source file:
 *
 * ```kotlin
 * class MyTest : InlineSourceTestEnvironment {
 *     @Test
 *     fun `test - my unit`() {
 *         val moduleDescriptor = createModuleDescriptor("""
 *             package com.example
 *             class MyClassUnderTest
 *         """.trimIndent()
 *         )
 *
 *         // my test!
 *     }
 * }
 * ```
 *
 * ## Test that requires several source files (e.g. multiple packages are involved)
 * ```kotlin
 * @Test
 * fun `test - my unit`() {
 *     val moduleDescriptor = createModuleDescriptor {
 *         source("class Foo")
 *         source("""
 *             package com.example
 *             class Foo
 *         """)
 *     }
 * }
 * ```
 */
interface InlineSourceTestEnvironment {
    val kotlinCoreEnvironment: KotlinCoreEnvironment
    val testDisposable: Disposable
    val testTempDir: File
}

interface InlineSourceCodeCollector {
    fun source(@Language("kotlin") sourceCode: String)
}

fun InlineSourceTestEnvironment.createModuleDescriptor(@Language("kotlin") sourceCode: String): ModuleDescriptor =
    createModuleDescriptor(kotlinCoreEnvironment, testTempDir, listOf(sourceCode))

fun InlineSourceTestEnvironment.createModuleDescriptor(build: InlineSourceCodeCollector.() -> Unit): ModuleDescriptor {
    val sources = mutableListOf<String>()
    object : InlineSourceCodeCollector {
        override fun source(sourceCode: String) {
            sources.add(sourceCode)
        }
    }.build()

    return createModuleDescriptor(kotlinCoreEnvironment, testTempDir, sources.toList())
}
