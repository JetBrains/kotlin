/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_OVERLOADS_FQ_NAME

internal fun IrPluginContext.generateNoArgConstructorBody(
    ctor: IrConstructor,
    ownerClass: IrClass,
    superConstructor: IrConstructor,
    invokeInitializers: Boolean
) {
    ctor.body = irFactory.createBlockBody(
        ctor.startOffset, ctor.endOffset,
        listOfNotNull(
            IrDelegatingConstructorCallImpl(
                ctor.startOffset, ctor.endOffset, irBuiltIns.unitType,
                superConstructor.symbol, 0,
            ),
            IrInstanceInitializerCallImpl(
                ctor.startOffset, ctor.endOffset, ownerClass.symbol, irBuiltIns.unitType
            ).takeIf { invokeInitializers }
        )
    )
}

// Returns true if this constructor is callable with no arguments by JVM rules, i.e. will have descriptor `()V`.
internal fun IrConstructor.isZeroParameterConstructor(): Boolean {
    val valueParameters = parameters.filter { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }
    return valueParameters.all { it.defaultValue != null } && (valueParameters.isEmpty() || isPrimary || hasAnnotation(
        JVM_OVERLOADS_FQ_NAME
    ))
}
