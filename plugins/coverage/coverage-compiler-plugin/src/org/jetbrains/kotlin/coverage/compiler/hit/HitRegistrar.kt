/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.jetbrains.kotlin.coverage.compiler.hit

import org.jetbrains.kotlin.coverage.compiler.common.KotlinCoverageInstrumentationContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

internal class HitRegistrarFactory {
    fun create(moduleId: Int, segmentNumber: Int, context: KotlinCoverageInstrumentationContext): HitRegistrar {
        // now we support only boolean hits
        return BooleanHitRegistrar(moduleId, segmentNumber, context)
    }
}

internal interface HitRegistrar {
    val extraDeclarations: List<IrDeclaration>
    fun body(irFunction: IrFunction): BlockWithExecutionPoints
    fun finalize()
}

internal interface BlockWithExecutionPoints {
    val firstStatement: IrStatement
    val pointsCount: Int
    fun registerPoint(): ExecutionPoint
}

internal class ExecutionPoint(
    val id: Int,
    val hitStatement: IrStatement,
)


