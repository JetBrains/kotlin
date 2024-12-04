/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge.impl

import org.jetbrains.kotlin.sir.bridge.BridgePrinter
import org.jetbrains.kotlin.sir.bridge.FunctionBridge
import org.jetbrains.kotlin.sir.bridge.GeneratedBridge
import org.jetbrains.kotlin.sir.bridge.TypeBindingBridge

internal class KotlinBridgePrinter : BridgePrinter {

    private val imports = mutableSetOf<String>()
    private val functions = mutableSetOf<List<String>>()
    private val fileLevelAnnotations = mutableSetOf<String>(
        """kotlin.Suppress("DEPRECATION_ERROR")""",
    )

    override fun add(bridge: GeneratedBridge) {
        when (bridge) {
            is FunctionBridge -> add(bridge)
            is TypeBindingBridge -> add(bridge)
        }
    }

    private fun add(bridge: FunctionBridge) {
        functions += bridge.kotlinFunctionBridge.lines
        imports += bridge.kotlinFunctionBridge.packageDependencies
    }

    private fun add(bridge: TypeBindingBridge) {
        fileLevelAnnotations += bridge.kotlinFileAnnotation
    }

    override fun print(): Sequence<String> = sequence {
        if (fileLevelAnnotations.isNotEmpty()) {
            fileLevelAnnotations.forEach {
                yield("@file:$it")
            }
            yield("")
        }
        if (imports.isNotEmpty()) {
            imports.forEach {
                yield("import $it")
            }
            yield("")
        }
        functions.forEach { functionLines ->
            yieldAll(functionLines)
            yield("")
        }
    }
}