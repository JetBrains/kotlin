/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer.impl

import org.jetbrains.kotlin.sir.*

internal class CBridgePrinter {

    private val includes = mutableSetOf<String>("Foundation/Foundation.h")

    private val functions = mutableSetOf<List<String>>()

    fun add(bridge: SirBridge) {
        when (bridge) {
            is SirFunctionBridge -> add(bridge)
            is SirTypeBindingBridge -> Unit
        }
    }

    private fun add(bridge: SirFunctionBridge) {
        functions += bridge.cDeclarationBridge.lines
        includes += bridge.cDeclarationBridge.headerDependencies
    }

    fun print(): Sequence<String> = sequence {
        includes.forEach {
            yield("#include <$it>")
        }
        yield("")

        yield("NS_ASSUME_NONNULL_BEGIN\n")

        functions.forEach { functionLines ->
            yieldAll(functionLines)
            yield("")
        }

        yield("NS_ASSUME_NONNULL_END")
    }
}