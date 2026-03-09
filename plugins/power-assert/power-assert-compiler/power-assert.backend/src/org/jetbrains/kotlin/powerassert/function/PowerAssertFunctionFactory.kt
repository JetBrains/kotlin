/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

var IrSimpleFunction.powerAssertDispatchSymbol: IrSimpleFunctionSymbol? by irAttribute(copyByDefault = false)

class PowerAssertFunctionFactory(
    private val context: IrPluginContext,
    private val builtIns: PowerAssertBuiltIns,
) {
    private val memorizedClassMetadata = hashSetOf<IrClass>()

    fun find(function: IrSimpleFunction): IrSimpleFunctionSymbol? {
        // If there is an explained symbol:
        // 1. Function was transformed to generate an explained overload.
        // 2. Explained overload was already found and saved for faster lookup.
        function.powerAssertDispatchSymbol?.let { return it }

        // Metadata indicates the function was transformed but is not in the current compilation unit.
        // If there is no metadata, the function was never compiled with power-assert.
        val parentClass = function.parent as? IrClass
        getPowerAssertMetadata(parentClass ?: function) ?: return null

        // Generate a stub-function so a symbol exists which can be called.
        return generate(function).symbol
    }

    fun generate(originalFunction: IrSimpleFunction): IrSimpleFunction {
        originalFunction.powerAssertDispatchSymbol?.let { return it.owner }

        val powerAssertFunction = originalFunction.deepCopyWithSymbols(originalFunction.parent)
        powerAssertFunction.apply {
            origin = FUNCTION_FOR_POWER_ASSERT
            name = Name.identifier("${originalFunction.name.identifier}\$powerassert")
            annotations = annotations.filter { !it.isAnnotation(PowerAssertBuiltIns.powerAssertFqName) }
            annotations += createJvmSyntheticAnnotation()
            val explanationParameter = addValueParameter {
                name = Name.identifier("\$explanation")
                type = builtIns.function0CallExplanationType
            }

            overriddenSymbols = originalFunction.overriddenSymbols.map { generate(it.owner).symbol }

            // Transform the generated function to use the `$explanation` parameter instead of CallExplain.explanation.
            transformChildrenVoid(PowerAssertGetExplanationTransformer(builtIns, explanationParameter))
            // Transform the generated function to propagate the `$explanation` parameter during recursive or super-calls.
            transformChildrenVoid(PowerAssertSelfCallTransformer(originalFunction, powerAssertFunction, explanationParameter))
        }

        // Save the power-assert function to the original function to make overload lookup easier.
        originalFunction.powerAssertDispatchSymbol = powerAssertFunction.symbol

        // Write custom metadata to indicate the original function was indeed compiled with the plugin.
        // This allows callers to be confident the power-assert function exists, even when calling from a different compilation unit.
        val parentClass = originalFunction.parent as? IrClass
        when {
            parentClass == null -> addPowerAssertMetadata(originalFunction)
            memorizedClassMetadata.add(parentClass) -> addPowerAssertMetadata(parentClass)
        }

        return powerAssertFunction
    }

    private fun createJvmSyntheticAnnotation(): IrAnnotation {
        val symbol = builtIns.jvmSyntheticAnnotation
        return IrAnnotationImpl.fromSymbolOwner(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = symbol.owner.parentAsClass.defaultType,
            constructorSymbol = symbol,
        )
    }

    private fun <E> addPowerAssertMetadata(declaration: E)
            where E : IrDeclaration, E : IrMetadataSourceOwner {
        if (declaration.metadata != null) {
            context.metadataDeclarationRegistrar
                .addCustomMetadataExtension(declaration, PowerAssertBuiltIns.PLUGIN_ID, builtIns.metadata.data)
        }
    }

    private fun <E> getPowerAssertMetadata(declaration: E): ByteArray?
            where E : IrDeclaration, E : IrMetadataSourceOwner {
        return context.metadataDeclarationRegistrar.getCustomMetadataExtension(declaration, PowerAssertBuiltIns.PLUGIN_ID)
    }
}
