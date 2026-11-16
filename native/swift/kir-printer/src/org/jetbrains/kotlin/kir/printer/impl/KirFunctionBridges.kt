/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.printer.impl

import org.jetbrains.kotlin.kir.KirBridged
import org.jetbrains.kotlin.kir.KirDeclaration
import org.jetbrains.kotlin.kir.KirDeclarationContainer
import org.jetbrains.kotlin.kir.KirFunctionBridge
import org.jetbrains.kotlin.kir.KirModule

/**
 * All unique function bridges of the declarations of [this] module, including the members of classes, in the order of declarations.
 */
internal fun KirModule.functionBridges(): List<KirFunctionBridge> = allDeclarations()
    .filterIsInstance<KirBridged>()
    .flatMap { it.bridges }
    .filterIsInstance<KirFunctionBridge>()
    .distinctBy { it.name }

/**
 * All declarations of [this] container, including the nested ones, in the order of declarations.
 */
internal fun KirDeclarationContainer.allDeclarations(): List<KirDeclaration> = declarations.flatMap { declaration ->
    listOf(declaration) + ((declaration as? KirDeclarationContainer)?.allDeclarations() ?: emptyList())
}

