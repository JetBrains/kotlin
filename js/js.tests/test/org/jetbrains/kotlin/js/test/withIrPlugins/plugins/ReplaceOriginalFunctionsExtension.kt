/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.withIrPlugins.plugins

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.remapTypeParameters
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerIr
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.module
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm

@ObsoleteDescriptorBasedAPI
class ReplaceOriginalFunctionsExtension : IrGenerationExtension {

    companion object {
        private const val ORIGINAL_SUFFIX = "_original"
        private const val MODIFIED_SUFFIX = "_generated"
    }

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {

        moduleFragment.transformChildrenVoid(CreateAndAddModifiedDeclarationsTransformer(pluginContext))
        moduleFragment.transformChildrenVoid(ReplaceOriginalCallsTransformer(pluginContext))
    }

    private class CreateAndAddModifiedDeclarationsTransformer(
        private val pluginContext: IrPluginContext
    ) : IrElementTransformerVoid() {

        private val typeString = pluginContext.irBuiltIns.stringType

        override fun visitFile(declaration: IrFile): IrFile {
            declaration.declarations.filterIsInstance<IrSimpleFunction>().filter {
                it.name.asString().endsWith(ORIGINAL_SUFFIX)
            }.forEach { f ->
                val newDeclaration = duplicateDeclaration(f as IrFunction)
                declaration.addChild(newDeclaration)
            }
            return super.visitFile(declaration)
        }

        private fun duplicateDeclaration(function: IrFunction): IrFunction {
            val originalName = function.name.asString()
            val newName = originalName.replace(ORIGINAL_SUFFIX, MODIFIED_SUFFIX)

            return pluginContext.irFactory.buildFun {
                name = Name.identifier(newName)
                visibility = DescriptorVisibilities.PUBLIC // default
                modality = Modality.FINAL // default
                returnType = typeString
            }.also {
                it.body = DeclarationIrBuilder(pluginContext, it.symbol).irBlockBody {
                    +irReturn(irString(newName))
                }
                it.parent = function.parent
            }
        }
    }

    @ObsoleteDescriptorBasedAPI
    private class ReplaceOriginalCallsTransformer(
        private val pluginContext: IrPluginContext
    ) : IrElementTransformerVoid() {

        private val signatureBuilder = when {
            pluginContext.platform.isJs() -> IdSignatureSerializer(JsManglerIr)
            pluginContext.platform.isJvm() -> IdSignatureSerializer(JvmManglerIr)
            else -> error("unsupported platform")
        }

        override fun visitCall(expression: IrCall): IrExpression {
            val callee: IrSimpleFunction = expression.symbol.owner
            val callName = callee.name.asString()

            if (!callName.endsWith(ORIGINAL_SUFFIX)) {
                return super.visitCall(expression)
            }

            val newName = callName.replace(ORIGINAL_SUFFIX, MODIFIED_SUFFIX)
            val method = (callee.parent as? IrDeclarationContainer)?.let {
                it.declarations.filterIsInstance<IrSimpleFunction>().firstOrNull {
                    it.name.asString() == newName
                }
            }
            if (method != null) {
                return DeclarationIrBuilder(pluginContext, expression.symbol).irCall(method)
            }

            val targetCallSignature = callee.copyWithName(
                context = pluginContext,
                newName = Name.identifier(newName)
            ).let {
                it.parent = callee.parent
                signatureBuilder.composePublicIdSignature(it) as IdSignature.PublicSignature
            }

            val symbol = pluginContext.referenceTopLevel(
                signature = targetCallSignature,
                kind = IrDeserializer.TopLevelSymbolKind.FUNCTION_SYMBOL,
                moduleDescriptor = callee.module
            ) as IrSimpleFunctionSymbol

            return DeclarationIrBuilder(pluginContext, expression.symbol).irCall(symbol.owner)
        }

        private fun IrSimpleFunction.copyWithName(context: IrPluginContext, newName: Name): IrFunction {
            val original = this
            val newFunction = context.irFactory.buildFun {
                updateFrom(original)
                name = newName
                returnType = original.returnType
            }
            newFunction.annotations = original.annotations
            newFunction.metadata = original.metadata
            newFunction.overriddenSymbols = original.overriddenSymbols
            newFunction.correspondingPropertySymbol = null
            newFunction.origin = IrDeclarationOrigin.DEFINED

            // here generic value parameters will be applied
            newFunction.copyParameterDeclarationsFrom(original)

            // ..but we need to remap the return type as well
            newFunction.returnType = newFunction.returnType.remapTypeParameters(
                source = original,
                target = newFunction
            )
            // remove leading $ in params to avoid confusing other transforms
            newFunction.valueParameters = newFunction.valueParameters

            return newFunction
        }
    }
}