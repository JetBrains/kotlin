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
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name

var IrSimpleFunction.explainedDispatchSymbol: IrSimpleFunctionSymbol? by irAttribute(followAttributeOwner = false)

class ExplainCallFunctionFactory(
    private val context: IrPluginContext,
    private val builtIns: PowerAssertBuiltIns,
) {
    private val memorizedClassMetadata = mutableSetOf<IrClass>()

    fun find(function: IrSimpleFunction): IrSimpleFunctionSymbol? {
        // If there is an explained symbol:
        // 1. Function was transformed to generate an explained overload.
        // 2. Explained overload was already found and saved for faster lookup.
        function.explainedDispatchSymbol?.let { return it }

        // Metadata indicates the function was transformed but is not in the current compilation unit.
        // Generate a stub-function so a symbol exists which can be called.
        val parentClass = function.parent as? IrClass
        getPowerAssertMetadata(parentClass ?: function) ?: return null
        return generate(function).symbol
    }

    fun generate(originalFunction: IrSimpleFunction): IrSimpleFunction {
        originalFunction.explainedDispatchSymbol?.let { return it.owner }

        val explainedFunction = originalFunction.deepCopyWithSymbols(originalFunction.parent)
        explainedFunction.apply {
            origin = FUNCTION_FOR_EXPLAIN_CALL
            name = Name.identifier("${originalFunction.name.identifier}\$explained")
            annotations = annotations.filter { !it.isAnnotation(PowerAssertBuiltIns.explainCallFqName) }
            annotations += createJvmSyntheticAnnotation() // TODO is this needed?
            val explanationParameter = addValueParameter {
                name = Name.identifier("\$explanation") // TODO what if there's another property with this name?
                type = builtIns.callExplanationType
            }

            overriddenSymbols = originalFunction.overriddenSymbols.map { generate(it.owner).symbol }

            // Transform the generated function to use the `$explanation` parameter instead of CallExplain.explanation.
            transformChildrenVoid(ExplainCallGetExplanationTransformer(builtIns, explanationParameter))
            // Transform the generated function to propagate the `$explanation` parameter during recursive or super-calls.
            transformChildrenVoid(ExplainedSelfCallTransformer(originalFunction, explanationParameter))

        }

        // Save the explained function to the original function to make overload lookup easier.
        originalFunction.explainedDispatchSymbol = explainedFunction.symbol

        // Write custom metadata to indicate the original function was indeed compiled with the plugin.
        // This allows callers to be confident the explained function exists, even when calling from a different compilation unit.
        val parentClass = originalFunction.parent as? IrClass
        when {
            parentClass == null -> addPowerAssertMetadata(originalFunction)
            memorizedClassMetadata.add(parentClass) -> addPowerAssertMetadata(parentClass)
        }

        return explainedFunction
    }

    private fun createJvmSyntheticAnnotation(): IrConstructorCallImpl {
        val symbol = context.referenceConstructors(JvmStandardClassIds.JVM_SYNTHETIC_ANNOTATION_CLASS_ID).single()
        return IrConstructorCallImpl.fromSymbolOwner(
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

