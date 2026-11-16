/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kir.lightclasses.utils

import org.jetbrains.kotlin.kir.CFunctionBridge
import org.jetbrains.kotlin.kir.KirBridge
import org.jetbrains.kotlin.kir.KirFunctionBridge
import org.jetbrains.kotlin.kir.KotlinFunctionBridge
import org.jetbrains.kotlin.kir.SwiftFunctionBridge
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirCallable
import org.jetbrains.kotlin.sir.SirFunctionBridge
import org.jetbrains.kotlin.sir.SirReverseFunctionBridge
import org.jetbrains.kotlin.sir.SirTypeBindingBridge

/**
 * The name of the bridge that Kotlin calls to invoke [this] Swift callable, or `null` if there is no such bridge.
 */
internal val SirCallable.reverseBridgeName: String?
    get() = bridges.filterIsInstance<SirReverseFunctionBridge>().singleOrNull()?.name

internal fun SirBridge.toKirBridge(): KirBridge = when (this) {
    is SirReverseFunctionBridge -> KirFunctionBridge(
        name = name,
        kotlinFunctionBridge = KotlinFunctionBridge(
            lines = kotlinFunctionBridge.lines,
            packageDependencies = kotlinFunctionBridge.packageDependencies,
        ),
        swiftFunctionBridge = SwiftFunctionBridge(lines = swiftFunctionBridge.lines),
        cDeclarationBridge = CFunctionBridge(
            lines = cDeclarationBridge.lines,
            headerDependencies = cDeclarationBridge.headerDependencies,
        ),
    )
    is SirFunctionBridge, is SirTypeBindingBridge -> error("Bridge '$name' is not supported in Swift import yet")
}
