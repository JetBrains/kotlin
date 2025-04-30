/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge.impl

import org.jetbrains.kotlin.sir.bridge.BridgePrinter
import org.jetbrains.kotlin.sir.bridge.FunctionBridge
import org.jetbrains.kotlin.sir.bridge.GeneratedBridge
import org.jetbrains.kotlin.sir.bridge.TypeBindingBridge

internal class CBridgePrinter : BridgePrinter {

    private val includes = mutableSetOf<String>("Foundation/Foundation.h")

    private val functions = mutableSetOf<List<String>>()

    override fun add(bridge: GeneratedBridge) {
        when (bridge) {
            is FunctionBridge -> add(bridge)
            is TypeBindingBridge -> Unit
        }
    }

    private fun add(bridge: FunctionBridge) {
        functions += bridge.cDeclarationBridge.lines
        includes += bridge.cDeclarationBridge.headerDependencies
    }

    override fun print(): Sequence<String> = sequence {
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