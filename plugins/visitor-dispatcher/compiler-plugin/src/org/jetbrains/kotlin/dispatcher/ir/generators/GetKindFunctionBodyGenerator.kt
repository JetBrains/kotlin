/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dispatcher.ir.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.common.AbstractFunctionBodyGenerator
import org.jetbrains.kotlin.dispatcher.common.FqnUtils
import org.jetbrains.kotlin.dispatcher.fir.AddGetKindFunctionExtension
import org.jetbrains.kotlin.dispatcher.ir.*
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.interpreter.getAnnotation

class GetKindFunctionBodyGenerator(private val dispatcherContext: DispatcherPluginContext, context: IrPluginContext): AbstractFunctionBodyGenerator(context) {
    override fun interestedIn(key: GeneratedDeclarationKey): Boolean {
        return key == AddGetKindFunctionExtension.Key
    }

    override fun generateBodyForFunction(function: IrSimpleFunction, key: GeneratedDeclarationKey): IrBody {
        val annotation = function.getAnnotation(FqnUtils.Kind.WITH_KIND_ANNOTATION_FQN)
        val enumType = getConstructorTypeArgument(annotation, 0)
        require(enumType != null)

        val enumEntryName = annotation.getConstValueArgument(0, IrConstKind.String)
        val enumEntry = dispatcherContext.enumEntryProvider.getEnumEntryByName(enumType, enumEntryName)
        require(enumEntry != null)

        val body = context.makeDeclBuilder(function.symbol).irBlockBody {
            +irReturn(
                IrGetEnumValueImpl(-1, -1, enumType, enumEntry.symbol)
            )
        }

        return body
    }
}