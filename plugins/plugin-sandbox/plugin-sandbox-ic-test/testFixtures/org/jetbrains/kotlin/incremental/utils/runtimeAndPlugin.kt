/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.utils

import org.jetbrains.kotlin.incremental.findJar
import org.jetbrains.kotlin.incremental.findKlib

private const val ANNOTATIONS_LIB_DIR = "plugins/plugin-sandbox/plugin-annotations/build/libs/"
private const val ANNOTATIONS_LIB_NAME = "plugin-annotations"

private const val PLUGIN_JAR_DIR = "plugins/plugin-sandbox/build/libs/"
private const val PLUGIN_JAR_NAME = "plugin-sandbox"

fun findAnnotationsRuntimeJar() = findJar(ANNOTATIONS_LIB_DIR, ANNOTATIONS_LIB_NAME, ":plugins:plugin-sandbox:plugin-annotations:jar")
fun findAnnotationsRuntimeJsKlib() = findKlib(ANNOTATIONS_LIB_DIR, ANNOTATIONS_LIB_NAME, ":plugins:plugin-sandbox:plugin-annotations:jsJar", platform = "js")
fun findAnnotationsRuntimeWasmKlib() = findKlib(ANNOTATIONS_LIB_DIR, ANNOTATIONS_LIB_NAME, ":plugins:plugin-sandbox:plugin-annotations:wasmJsJar", platform = "wasm-js")

fun findPluginJar() = findJar(PLUGIN_JAR_DIR, PLUGIN_JAR_NAME, ":plugins:plugin-sandbox:jar", isCompilerPlugin = true)
