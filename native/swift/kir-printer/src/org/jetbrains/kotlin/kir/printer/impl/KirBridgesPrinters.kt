/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.printer.impl

import org.jetbrains.kotlin.kir.KirModule

/**
 * Prints Swift implementations of the bridges of [module].
 */
internal fun renderSwiftBridges(module: KirModule): String {
    val bridges = module.functionBridges()
    if (bridges.isEmpty()) return ""
    return bridges.joinToString(separator = "\n\n", postfix = "\n") { it.swiftFunctionBridge.lines.joinToString("\n") }
}

/**
 * Prints C declarations of the bridges of [module].
 */
internal fun renderCDeclarations(module: KirModule): String {
    val bridges = module.functionBridges()
    if (bridges.isEmpty()) return ""
    val includes = bridges.flatMap { it.cDeclarationBridge.headerDependencies }.distinct().map { "#include <$it>" }
    val declarations = bridges.flatMap { it.cDeclarationBridge.lines }
    return (includes + "" + declarations).joinToString(separator = "\n", postfix = "\n")
}
