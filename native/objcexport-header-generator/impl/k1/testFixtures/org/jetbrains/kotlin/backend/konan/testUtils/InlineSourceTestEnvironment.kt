/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import com.intellij.openapi.Disposable
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import java.io.File

interface InlineSourceTestEnvironment {
    val testDisposable: Disposable
    val testTempDir: File
}

interface InlineSourceCodeCollector {
    fun source(@Language("kotlin") sourceCode: String)
}

fun InlineSourceTestEnvironment.compileAndDeserializeSources(@Language("kotlin") sourceCode: String): ModuleDescriptor =
    compileAndDeserializeSources(testTempDir, listOf(sourceCode))

fun InlineSourceTestEnvironment.compileAndDeserializeSources(build: InlineSourceCodeCollector.() -> Unit): ModuleDescriptor {
    val sources = mutableListOf<String>()
    object : InlineSourceCodeCollector {
        override fun source(sourceCode: String) {
            sources.add(sourceCode)
        }
    }.build()

    return compileAndDeserializeSources(testTempDir, sources.toList())
}
