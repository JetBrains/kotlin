/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstantValue
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCatchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.checkers.isRestrictsSuspensionReceiver
import org.jetbrains.kotlin.resolve.scopes.MemberScope

private var topLevelInitializersCounter = 0

internal fun IrFile.addTopLevelInitializer(expression: IrExpression, context: KonanBackendContext, threadLocal: Boolean, eager: Boolean) {
    val irField = IrFieldImpl(
            expression.startOffset, expression.endOffset,
            IrDeclarationOrigin.DEFINED,
            IrFieldSymbolImpl(),
            "topLevelInitializer${topLevelInitializersCounter++}".synthesizedName,
            expression.type,
            DescriptorVisibilities.PRIVATE,
            isFinal = true,
            isExternal = false,
            isStatic = true,
    ).apply {
        expression.setDeclarationsParent(this)

        if (threadLocal)
            annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, context.ir.symbols.threadLocal.owner)

        if (eager)
            annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, context.ir.symbols.eagerInitialization.owner)

        initializer = IrExpressionBodyImpl(startOffset, endOffset, expression)
    }
    addChild(irField)
}

fun IrBuilderWithScope.irCatch() =
        IrCatchImpl(
                startOffset, endOffset,
                buildVariable(
                        this@irCatch.parent,
                        startOffset,
                        endOffset,
                        IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                        Name.identifier("e"),
                        context.irBuiltIns.throwableType,
                        isVar = false,
                        isConst = false,
                        isLateinit = false
                )
        )

fun IrBuilderWithScope.irByte(value: Byte) =
        IrConstImpl.byte(startOffset, endOffset, context.irBuiltIns.byteType, value)

val IrField.hasNonConstInitializer: Boolean
    get() = initializer?.expression.let { it != null && it !is IrConst<*> && it !is IrConstantValue }
