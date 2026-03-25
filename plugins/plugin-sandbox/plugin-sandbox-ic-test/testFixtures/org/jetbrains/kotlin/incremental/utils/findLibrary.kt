/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.incremental

import org.junit.jupiter.api.fail
import java.io.File
import java.util.jar.JarFile
import kotlin.io.extension
import kotlin.test.assertTrue

private fun findLib(
    dir: String,
    name: String,
    taskName: String,
    extension: String,
    isCompilerPlugin: Boolean
): String {
    val failMessage = { "$extension $name does not exist. Please run $taskName" }
    val libDir = File(dir)
    assertTrue(libDir.exists() && libDir.isDirectory)
    val jar = libDir.listFiles()?.firstOrNull {
        it.name.startsWith(name) && it.extension == extension && (!isCompilerPlugin || JarFile(it).containsCompilerPluginRegistrar())
    } ?: fail(failMessage)
    return jar.canonicalPath
}

private fun JarFile.containsCompilerPluginRegistrar(): Boolean =
    entries().toList().any { it.name.endsWith("org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar") }

internal fun findJar(dir: String, name: String, taskName: String, isCompilerPlugin: Boolean = false) =
    findLib(dir, name, taskName, "jar", isCompilerPlugin)

internal fun findKlib(dir: String, name: String, taskName: String) =
    findLib(dir, name, taskName, "klib", isCompilerPlugin = false)