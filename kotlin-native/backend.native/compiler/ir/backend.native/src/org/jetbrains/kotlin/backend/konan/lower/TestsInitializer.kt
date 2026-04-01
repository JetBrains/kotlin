/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.util.SetDeclarationsParentVisitor
import org.jetbrains.kotlin.ir.util.hasAnnotation
import kotlin.collections.plus

internal class TestsInitializer(private val context: Context) : FileLoweringPass {
    private val symbols = context.symbols

    override fun lower(irFile: IrFile) {
        for (idx in irFile.declarations.indices) {
            val function = irFile.declarations[idx] as? IrSimpleFunction ?: continue
            if (!function.hasAnnotation(symbols.testInitializer)) continue
            irFile.declarations[idx] = context.irFactory.buildField {
                startOffset = function.startOffset
                endOffset = function.endOffset
                name = "createTestSuites".synthesizedName
                visibility = DescriptorVisibilities.PRIVATE
                isFinal = true
                isStatic = true
                type = context.irBuiltIns.unitType
            }.apply {
                parent = irFile
                annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, symbols.eagerInitialization.owner)
                annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, symbols.threadLocal.owner)
                val statements = (function.body as IrBlockBody).statements
                statements.forEach { it.accept(SetDeclarationsParentVisitor, this) }
                initializer = context.irFactory.createExpressionBody(
                        startOffset, endOffset,
                        IrCompositeImpl(startOffset, endOffset, context.irBuiltIns.unitType, null, statements)
                )
            }
        }
    }
}