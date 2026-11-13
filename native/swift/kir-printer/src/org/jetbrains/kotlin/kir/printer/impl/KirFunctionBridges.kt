/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.printer.impl

import org.jetbrains.kotlin.kir.KirBridged
import org.jetbrains.kotlin.kir.KirFunctionBridge
import org.jetbrains.kotlin.kir.KirModule

/**
 * All unique function bridges of the declarations of [this] module, in the order of declarations.
 */
internal fun KirModule.functionBridges(): List<KirFunctionBridge> = declarations
    .filterIsInstance<KirBridged>()
    .flatMap { it.bridges }
    .filterIsInstance<KirFunctionBridge>()
    .distinctBy { it.name }
