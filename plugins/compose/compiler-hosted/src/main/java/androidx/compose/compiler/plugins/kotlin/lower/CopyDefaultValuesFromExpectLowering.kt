/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.hasComposableAnnotation
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor
import org.jetbrains.kotlin.resolve.multiplatform.findCompatibleActualsForExpected

/**
 * [ComposableFunctionBodyTransformer] relies on presence of default values in
 * Composable functions' parameters.
 * If Composable function is declared as `expect fun` with default value parameter, then
 * [ComposableFunctionBodyTransformer] will not find any default value in `actual fun` - IrFunction.
 *
 * [CopyDefaultValuesFromExpectLowering] sets default values to parameters of actual functions by
 * taking them from their corresponding `expect fun` declarations.
 * This lowering needs to run before [ComposableFunctionBodyTransformer] and
 * before [ComposerParamTransformer].
 *
 * Fixes:
 * https://github.com/JetBrains/compose-jb/issues/1407
 * https://github.com/JetBrains/compose-multiplatform/issues/2816
 * https://github.com/JetBrains/compose-multiplatform/issues/2806
 *
 * This implementation is borrowed from Kotlin's ExpectToActualDefaultValueCopier.
 * Currently, it heavily relies on descriptors to find expect for actuals or vice versa:
 * findCompatibleActualsForExpected.
 * Unlike ExpectToActualDefaultValueCopier, this lowering performs its transformations
 * only for functions marked with @Composable annotation or
 * for functions with @Composable lambdas in parameters.
 *
 * This lowering is K1 specific and should not be run in K2.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
class CopyDefaultValuesFromExpectLowering(
    private val pluginContext: IrPluginContext,
) : ModuleLoweringPass, IrElementTransformerVoid() {

    private val symbolTable = pluginContext.symbolTable

    private fun isApplicable(declaration: IrFunction): Boolean {
        return declaration.hasComposableAnnotation() ||
                declaration.valueParameters.any {
                    it.type.hasAnnotation(ComposeFqNames.Composable)
                }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val original = super.visitFunction(declaration) as? IrFunction ?: return declaration

        if (!original.isExpect || !isApplicable(original)) {
            return original
        }

        val actualForExpected = original.findActualForExpected()

        original.valueParameters.forEachIndexed { index, expectValueParameter ->
            val actualValueParameter = actualForExpected.valueParameters[index]
            val expectDefaultValue = expectValueParameter.defaultValue
            if (expectDefaultValue != null) {
                actualValueParameter.defaultValue = expectDefaultValue
                    .remapExpectValueSymbols()
                    .patchDeclarationParents(actualForExpected)

                // Remove a default value in the expect fun in order to prevent
                // Kotlin expect/actual-related lowerings trying to copy the default values again
                expectValueParameter.defaultValue = null
            }
        }
        return original
    }

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    private inline fun <reified T : IrFunction> T.findActualForExpected(): T {
        val symbol = symbolTable.referenceFunction(descriptor.findActualForExpect())
        if (!symbol.isBound) {
            // some actual symbols might be coming from incremental compilation and they are not
            // directly linked by default.
            (pluginContext as? IrPluginContextImpl)?.linker?.getDeclaration(symbol)
        }
        return symbol.owner as T
    }

    private fun IrProperty.findActualForExpected(): IrProperty =
        symbolTable.descriptorExtension.referenceProperty(
            descriptor.findActualForExpect()
        ).owner

    private fun IrClass.findActualForExpected(): IrClass =
        symbolTable.descriptorExtension.referenceClass(
            descriptor.findActualForExpect()
        ).owner

    private fun IrEnumEntry.findActualForExpected(): IrEnumEntry =
        symbolTable.descriptorExtension.referenceEnumEntry(
            descriptor.findActualForExpect()
        ).owner

    private inline fun <reified T : MemberDescriptor> T.findActualForExpect(): T {
        if (!this.isExpect) error(this)
        return (findCompatibleActualsForExpected(module).singleOrNull() ?: error(this)) as T
    }

    private fun IrExpressionBody.remapExpectValueSymbols(): IrExpressionBody {
        class SymbolRemapper : DeepCopySymbolRemapper() {
            override fun getReferencedClass(symbol: IrClassSymbol) =
                if (symbol.descriptor.isExpect)
                    symbol.owner.findActualForExpected().symbol
                else super.getReferencedClass(symbol)

            override fun getReferencedTypeParameter(symbol: IrTypeParameterSymbol): IrTypeParameterSymbol =
                remapExpectTypeParameter(symbol).symbol

            override fun getReferencedScript(symbol: IrScriptSymbol): IrScriptSymbol =
                error("Unexpected symbol $symbol ${symbol.descriptor}")

            override fun getReferencedConstructor(symbol: IrConstructorSymbol) =
                if (symbol.descriptor.isExpect)
                    symbol.owner.findActualForExpected().symbol
                else super.getReferencedConstructor(symbol)

            override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol) = when {
                symbol.descriptor.isExpect -> symbol.owner.findActualForExpected().symbol

                symbol.descriptor.propertyIfAccessor.isExpect -> {
                    val property = symbol.owner.correspondingPropertySymbol!!.owner
                    val actualPropertyDescriptor = property.descriptor.findActualForExpect()
                    val accessorDescriptor = when (symbol.owner) {
                        property.getter -> actualPropertyDescriptor.getter!!
                        property.setter -> actualPropertyDescriptor.setter!!
                        else -> error("Unexpected accessor of $symbol ${symbol.descriptor}")
                    }
                    symbolTable.referenceFunction(accessorDescriptor) as IrSimpleFunctionSymbol
                }

                else -> super.getReferencedSimpleFunction(symbol)
            }

            override fun getReferencedProperty(symbol: IrPropertySymbol) =
                if (symbol.descriptor.isExpect)
                    symbol.owner.findActualForExpected().symbol
                else
                    super.getReferencedProperty(symbol)

            override fun getReferencedEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol =
                if (symbol.descriptor.isExpect)
                    symbol.owner.findActualForExpected().symbol
                else
                    super.getReferencedEnumEntry(symbol)

            override fun getReferencedValue(symbol: IrValueSymbol) =
                remapExpectValue(symbol)?.symbol ?: super.getReferencedValue(symbol)
        }

        val symbolRemapper = SymbolRemapper()
        acceptVoid(symbolRemapper)

        return transform(DeepCopyIrTreeWithSymbols(symbolRemapper), null)
    }

    private fun remapExpectTypeParameter(symbol: IrTypeParameterSymbol): IrTypeParameter {
        val parameter = symbol.owner
        val parent = parameter.parent

        return when (parent) {
            is IrClass ->
                if (!parent.descriptor.isExpect)
                    parameter
                else parent.findActualForExpected().typeParameters[parameter.index]

            is IrFunction ->
                if (!parent.descriptor.isExpect)
                    parameter
                else parent.findActualForExpected().typeParameters[parameter.index]

            else -> error(parent)
        }
    }

    private fun remapExpectValue(symbol: IrValueSymbol): IrValueParameter? {
        if (symbol !is IrValueParameterSymbol) {
            return null
        }

        val parameter = symbol.owner
        val parent = parameter.parent

        return when (parent) {
            is IrClass ->
                if (!parent.descriptor.isExpect)
                    null
                else {
                    assert(parameter == parent.thisReceiver)
                    parent.findActualForExpected().thisReceiver!!
                }

            is IrFunction ->
                if (!parent.descriptor.isExpect)
                    null
                else when (parameter) {
                    parent.dispatchReceiverParameter ->
                        parent.findActualForExpected().dispatchReceiverParameter!!

                    parent.extensionReceiverParameter ->
                        parent.findActualForExpected().extensionReceiverParameter!!

                    else -> {
                        assert(parent.valueParameters[parameter.indexInOldValueParameters] == parameter)
                        parent.findActualForExpected().valueParameters[parameter.indexInOldValueParameters]
                    }
                }

            else -> error(parent)
        }
    }
}
