/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.DescriptorRenderer

val REPL_SNIPPET_EVAL_FUN_NAME = Name.identifier("\$\$eval")
val REPL_SNIPPET_RESULT_PROP_NAME = Name.identifier("\$\$result")

internal class ReplSnippetsToClassesLowering(val context: IrPluginContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val snippets = mutableListOf<IrReplSnippet>()

        for (irFile in irModule.files) {
            irFile.declarations.filterIsInstanceTo(snippets, IrReplSnippet::class.java)
            irFile.declarations.removeIf { it is IrReplSnippet }
        }

        val symbolRemapper = ReplSnippetsToClassesSymbolRemapper()

        snippets.sortBy { it.name }
        for (irSnippet in snippets) {
            finalizeReplSnippetClass(irSnippet, symbolRemapper)
        }
    }

    private fun finalizeReplSnippetClass(irSnippet: IrReplSnippet, symbolRemapper: ReplSnippetsToClassesSymbolRemapper) {
        val irSnippetClass = irSnippet.targetClass!!.owner
        val typeRemapper = SimpleTypeRemapper(symbolRemapper)

        val implicitReceiversFieldsWithParameters = makeImplicitReceiversFieldsWithParameters(irSnippetClass, typeRemapper, irSnippet)

        val irSnippetClassThisReceiver =
            irSnippet.createThisReceiverParameter(context, IrDeclarationOrigin.INSTANCE_RECEIVER, irSnippetClass.typeWith()).also {
                it.parent = irSnippetClass
            }
        irSnippetClass.thisReceiver = irSnippetClassThisReceiver

        val snippetAccessCallsGenerator = ReplSnippetAccessCallsGenerator(
            context, irSnippetClassThisReceiver, implicitReceiversFieldsWithParameters, irSnippetClass, irSnippet.stateObject!!
        )

        val evalFun = irSnippetClass.declarations
            .filterIsInstance<IrFunction>()
            .single { it.origin == IrDeclarationOrigin.REPL_EVAL_FUNCTION }
        evalFun.parameters = buildList {
            add(
                evalFun.buildReceiverParameter {
                    origin = irSnippetClass.origin
                    type = irSnippetClass.defaultType
                }
            )
            implicitReceiversFieldsWithParameters.forEach { (_, param) -> add(param) }
        }
        (evalFun.body as? IrBlockBody)?.statements?.add(
            index = 0,
            element = snippetAccessCallsGenerator.createPutSelfToState(
                ScriptLikeToClassTransformerContext.makeRootContext(
                    valueParameterForScriptThis = evalFun.dispatchReceiverParameter?.symbol,
                    isInScriptConstructor = false,
                    topLevelDeclarationWithScriptReceiver = evalFun
                )
            )
        )

        val resultProp = irSnippetClass.declarations
            .filterIsInstance<IrProperty>()
            .singleOrNull { it.origin == IrDeclarationOrigin.SCRIPT_RESULT_PROPERTY }
        resultProp?.let { irResultProperty ->
            val backingField = irResultProperty.backingField ?: return@let
            backingField.visibility = DescriptorVisibilities.PUBLIC

            val fieldType = backingField.type.toIrBasedKotlinType()
            irSnippetClass.scriptResultFieldDataAttr =
                ScriptResultFieldData(
                    irSnippetClass.kotlinFqName,
                    irResultProperty.name,
                    DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(fieldType)
                )
        }

        val scriptTransformer = ReplSnippetToClassTransformer(
            context,
            irSnippet,
            irSnippetClass,
            irSnippetClassThisReceiver,
            typeRemapper,
            snippetAccessCallsGenerator,
        )
        val lambdaPatcher = ScriptFixLambdasTransformer(irSnippetClass)

        irSnippetClass.declarations.transformInPlace {
            val rootContext =
                if (it is IrConstructor)
                    ScriptLikeToClassTransformerContext.makeRootContext(irSnippetClass.thisReceiver!!.symbol, true)
                else
                    ScriptLikeToClassTransformerContext.makeRootContext(null, isInScriptConstructor = false)
            it.transform(scriptTransformer, rootContext)
                .transform(lambdaPatcher, ScriptFixLambdasTransformerContext())
        }

        // TODO: find out what problems could arise from copying annotations applicable to file only (KT-74176)
        irSnippetClass.annotations += (irSnippetClass.parent as IrFile).annotations
    }
}

private class ReplSnippetsToClassesSymbolRemapper : SymbolRemapper.Empty() {
    override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol =
        (symbol.owner as? IrReplSnippet)?.targetClass ?: symbol
}

private class ReplSnippetAccessCallsGenerator(
    context: IrPluginContext,
    snippetClassReceiver: IrValueParameter,
    implicitReceiversFieldsWithParameters: List<Pair<IrField, IrValueParameter>>,
    val irSnippetClass: IrClass,
    val irReplStateObjectSymbol: IrClassSymbol
) : ScriptLikeAccessCallsGenerator(context, snippetClassReceiver, implicitReceiversFieldsWithParameters) {
    private val mapClass = irReplStateObjectSymbol.owner
    private val mapGet = mapClass.functions.single { it.name.asString() == "get" }
    private val mapPut = mapClass.functions.single { it.name.asString() == "put" }

    fun createIrGetValFromState(
        startOffset: Int,
        endOffset: Int,
        irSnippetClassFromState: IrClassSymbol,
        irTargetField: IrFieldSymbol
    ): IrExpression =
        IrGetFieldImpl(
            startOffset,
            endOffset,
            irTargetField,
            irTargetField.owner.type,
            IrStatementOrigin.GET_PROPERTY
        ).also {
            it.receiver =
                createAccessToSnippet(irSnippetClassFromState, startOffset, endOffset)
        }

    fun createIrSetValInState(
        startOffset: Int,
        endOffset: Int,
        irSnippetClassFromState: IrClassSymbol,
        irTargetField: IrFieldSymbol,
        expression: IrExpression
    ): IrExpression =
        IrSetFieldImpl(
            startOffset,
            endOffset,
            irTargetField,
            context.irBuiltIns.unitType,
        ).also {
            it.receiver = createAccessToSnippet(irSnippetClassFromState, startOffset, endOffset)
            it.value = expression
        }

    fun createPutSelfToState(data: ScriptLikeToClassTransformerContext): IrCall =
        IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, mapPut.returnType, mapPut.symbol).apply {
            arguments[0] =
                IrGetObjectValueImpl(
                    startOffset, endOffset,
                    irReplStateObjectSymbol.typeWith(),
                    irReplStateObjectSymbol,
                )
            arguments[1] =
                IrConstImpl.string(
                    startOffset,
                    endOffset,
                    context.irBuiltIns.stringType,
                    irSnippetClass.name.asString()
                )
            arguments[2] = getAccessCallForSelf(data, startOffset, endOffset, null, null)
        }

    fun createAccessToSnippet(
        irSnippetClassFromState: IrClassSymbol,
        startOffset: Int,
        endOffset: Int,
    ): IrExpression? {
        val getSnippetCall = IrCallImpl(startOffset, endOffset, mapGet.returnType, mapGet.symbol).apply {
            arguments[0] =
                IrGetObjectValueImpl(
                    startOffset, endOffset,
                    irReplStateObjectSymbol.typeWith(),
                    irReplStateObjectSymbol,
                )
            arguments[1] =
                IrConstImpl.string(
                    startOffset,
                    endOffset,
                    context.irBuiltIns.stringType,
                    irSnippetClassFromState.owner.name.asString()
                )
        }
        val targetType = irSnippetClassFromState.typeWith()
        return IrTypeOperatorCallImpl(
            startOffset, endOffset, targetType,
            IrTypeOperator.CAST,
            targetType, getSnippetCall
        )
    }

}

private class ReplSnippetToClassTransformer(
    context: IrPluginContext,
    val irSnippet: IrReplSnippet,
    irSnippetClass: IrClass,
    snippetClassReceiver: IrValueParameter,
    typeRemapper: TypeRemapper,
    override val accessCallsGenerator: ReplSnippetAccessCallsGenerator,
) : ScriptLikeToClassTransformer(
    context,
    irSnippet,
    irSnippetClass,
    snippetClassReceiver,
    typeRemapper,
    accessCallsGenerator,
    capturingClasses = emptySet(),
    needsReceiverProcessing = true
) {
    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: ScriptLikeToClassTransformerContext): IrExpression {
        val declaration = expression.symbol.owner as? IrDeclaration
        if (declaration != null && declaration in irSnippet.declarationsFromOtherSnippets) {
            expression.dispatchReceiver = accessCallsGenerator.createAccessToSnippet(
                (declaration.parent as IrClass).symbol,
                expression.startOffset, expression.endOffset
            )
            expression.transformChildren(this, data)
            return expression
        }
        return super.visitMemberAccess(expression, data)
    }

    override fun visitClass(declaration: IrClass, data: ScriptLikeToClassTransformerContext): IrClass {
        declaration.updateVisibilityToPublicIfNeeded()
        return super.visitClass(declaration, data)
    }

    override fun visitFunction(declaration: IrFunction, data: ScriptLikeToClassTransformerContext): IrStatement {
        declaration.updateVisibilityToPublicIfNeeded()
        return super.visitFunction(declaration, data)
    }

    override fun visitProperty(declaration: IrProperty, data: ScriptLikeToClassTransformerContext): IrStatement {
        declaration.updateVisibilityToPublicIfNeeded()
        return super.visitProperty(declaration, data)
    }
}

private fun IrDeclarationWithVisibility.updateVisibilityToPublicIfNeeded() {
    // The snippet top-level classes visibilities are set to public, so this function is used to update
    // visibilities of such class memebrs recursively, to avoid incorrect codegeneration
    if (visibility == DescriptorVisibilities.LOCAL &&
        parent.let { it is IrClass && it.visibility == DescriptorVisibilities.PUBLIC }
    ) {
        visibility = DescriptorVisibilities.PUBLIC
    }
}

private fun makeImplicitReceiversFieldsWithParameters(
    irSnippetClass: IrClass,
    typeRemapper: SimpleTypeRemapper,
    irSnippet: IrReplSnippet,
): List<Pair<IrField, IrValueParameter>> =
    irSnippet.receiverParameters.map { param ->
        val typeName = param.type.classFqName?.shortName()?.identifierOrNullIfSpecial
        irSnippetClass.addField {
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            origin = IrDeclarationOrigin.SCRIPT_IMPLICIT_RECEIVER
            name = Name.identifier("\$\$implicitReceiver_${typeName ?: param.indexInParameters.toString()}")
            visibility = DescriptorVisibilities.PRIVATE
            type = typeRemapper.remapType(param.type)
            isFinal = true
        } to param
    }