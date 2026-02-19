/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer.impl

import org.jetbrains.kotlin.sir.*
import org.jetbrains.sir.printer.*
import kotlin.collections.plusAssign

internal class KotlinBridgePrinter {

    private val imports = mutableSetOf<String>()
    private val functions = mutableSetOf<List<String>>()
    private val fileLevelAnnotations = mutableSetOf<String>(
        """kotlin.Suppress("DEPRECATION_ERROR")""",
    )

    fun add(bridge: SirBridge) {
        when (bridge) {
            is SirFunctionBridge -> add(bridge)
            is SirTypeBindingBridge -> add(bridge)
        }
    }

    private fun add(bridge: SirFunctionBridge) {
        functions += bridge.kotlinFunctionBridge.lines
        imports += bridge.kotlinFunctionBridge.packageDependencies
    }

    private fun add(bridge: SirTypeBindingBridge) {
        fileLevelAnnotations += bridge.kotlinFileAnnotation
    }

    fun print(): Sequence<String> = sequence {
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