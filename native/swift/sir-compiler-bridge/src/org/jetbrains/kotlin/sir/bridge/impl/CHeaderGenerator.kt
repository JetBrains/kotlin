/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge.impl

import org.jetbrains.kotlin.sir.bridge.BridgePrinter
import org.jetbrains.kotlin.sir.bridge.FunctionBridge

internal class CBridgePrinter : BridgePrinter {

    private val includes = mutableSetOf<String>()

    private val functions = mutableListOf<List<String>>()

    override fun add(bridge: FunctionBridge) {
        functions += bridge.cDeclarationBridge.lines
        includes += bridge.cDeclarationBridge.headerDependencies
    }

    override fun print(): Sequence<String> = sequence {
        if (includes.isNotEmpty()) {
            includes.forEach {
                yield("#include <$it>")
            }
            yield("")
        }
        functions.forEach { functionLines ->
            yieldAll(functionLines)
            yield("")
        }
    }
}