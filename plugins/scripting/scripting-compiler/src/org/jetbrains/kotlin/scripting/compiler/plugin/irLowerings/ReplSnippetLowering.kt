/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.originalSnippetValueSymbol
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.INTERNAL
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.name.SpecialNames

@PhaseDescription(name = "ReplSnippetsToClasses")
internal class ReplSnippetsToClassesLowering(val context: IrPluginContext) : ModuleLoweringPass {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun lower(irModule: IrModuleFragment) {
        val snippets = mutableListOf<IrReplSnippet>()

        for (irFile in irModule.files) {
            val iterator = irFile.declarations.listIterator()
            while (iterator.hasNext()) {
                val declaration = iterator.next()
                if (declaration is IrReplSnippet) {
                    val snippetClass = prepareReplSnippetClass(irFile, declaration)
                    snippets.add(declaration)
                    iterator.set(snippetClass)
                }
            }
        }

        val symbolRemapper = ReplSnippetsToClassesSymbolRemapper()

        snippets.sortBy { it.name }
        for (irSnippet in snippets) {
            finalizeReplSnippetClass(irSnippet, symbolRemapper)
            irSnippet.targetClass!!.owner.patchDeclarationParents(irSnippet.parent)
        }
    }

    private fun prepareReplSnippetClass(irFile: IrFile, irSnippet: IrReplSnippet): IrClass {
        val fileEntry = irFile.fileEntry
        return context.irFactory.buildClass {
            startOffset = 0
            endOffset = fileEntry.maxOffset
            origin = IrDeclarationOrigin.REPL_SNIPPET_CLASS
            name = NameUtils.getScriptTargetClassName(irSnippet.name)
            kind = ClassKind.OBJECT
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
        }.also { irSnippetClass ->
            irSnippetClass.superTypes += (context.irBuiltIns.anyNType)
            irSnippetClass.parent = irFile
            irSnippetClass.metadata = irSnippet.metadata
            irSnippet.targetClass = irSnippetClass.symbol
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun finalizeReplSnippetClass(irSnippet: IrReplSnippet, symbolRemapper: ReplSnippetsToClassesSymbolRemapper) {

        val irSnippetClass = irSnippet.targetClass!!.owner
        val typeRemapper = SimpleTypeRemapper(symbolRemapper)

        val implicitReceiversFieldsWithParameters = makeImplicitReceiversFieldsWithParameters(irSnippetClass, typeRemapper, irSnippet)

        val valsToFields = mutableMapOf<IrVariableSymbol, IrFieldSymbol>()

        val irSnippetClassType = IrSimpleTypeImpl(irSnippetClass.symbol, false, emptyList(), emptyList())
        val irSnippetClassThisReceiver = irSnippet.createThisReceiverParameter(context, IrDeclarationOrigin.INSTANCE_RECEIVER, irSnippetClassType)
        irSnippetClass.thisReceiver = irSnippetClassThisReceiver

        val snippetAccessCallsGenerator = ReplSnippetAccessCallsGenerator(
            context, irSnippetClassThisReceiver, implicitReceiversFieldsWithParameters, irSnippetClass, irSnippet.stateObject!!
        )

        irSnippetClass.declarations.add(createConstructor(irSnippetClass))

        irSnippetClass.addFunction {
            name = Name.identifier("eval")
            startOffset = irSnippet.startOffset
            endOffset = irSnippet.endOffset
            returnType = context.irBuiltIns.unitType // TODO: implement value returning
            visibility = INTERNAL
        }.also { evalFun ->
            evalFun.parent = irSnippetClass
            evalFun.dispatchReceiverParameter =
                evalFun.buildReceiverParameter {
                    origin = irSnippetClass.origin
                    type = irSnippetClass.defaultType
                }
            evalFun.body =
                context.irBuiltIns.createIrBuilder(evalFun.symbol).irBlockBody {
                    val flattenedStatements = irSnippet.body.statements.flatMap { snippetStatement ->
                        if (snippetStatement is IrComposite) {
                            snippetStatement.statements
                        } else {
                            listOf(snippetStatement)
                        }
                    }
                    // relies on that Fir2IrVisitor.convertToIrBlockBody insert implicit coercion calls for "unused" statements
                    val lastExpression = (flattenedStatements.lastOrNull() as? IrTypeOperatorCall)?.takeIf {
                        it.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
                    }
                    var lastExpressionVar: IrVariable? = null
                    flattenedStatements.forEach { statement ->
                        if (statement == lastExpression) {
                            // Could become a `$res..` one
                            lastExpressionVar = createTmpVariable(lastExpression.argument, nameHint = "result")
                        } else {
                            when (statement) {
                                is IrVariable -> {
                                    irSnippetClass.addField {
                                        startOffset = statement.startOffset
                                        endOffset= statement.endOffset
                                        name = statement.name
                                        type = statement.type
                                        visibility = DescriptorVisibilities.PUBLIC
                                        origin = IrDeclarationOrigin.DEFINED
                                    }.also { field ->
                                        statement.initializer?.let { initializer ->
                                            +IrSetFieldImpl(
                                                initializer.startOffset,
                                                initializer.endOffset,
                                                field.symbol,
                                                irGet(irSnippetClassThisReceiver),
                                                initializer,
                                                this.context.irBuiltIns.unitType
                                            )
                                        }
                                        valsToFields[statement.symbol] = field.symbol
                                    }
                                }
                                is IrProperty,
                                is IrSimpleFunction,
                                is IrClass -> {
                                    statement.visibility = DescriptorVisibilities.PUBLIC
                                    irSnippetClass.declarations.add(statement)
                                }
                                else -> {
                                    +statement
                                }
                            }
                        }
                    }
                    +snippetAccessCallsGenerator.createPutSelfToState(
                        ScriptLikeToClassTransformerContext.makeRootContext(
                            valueParameterForScriptThis = evalFun.dispatchReceiverParameter?.symbol,
                            isInScriptConstructor = false,
                            topLevelDeclaration = evalFun
                        )
                    )
                    lastExpression?.let {
                        +irReturn(IrGetValueImpl(it.startOffset, it.endOffset, lastExpressionVar!!.symbol))
                    }
                    evalFun.returnType = lastExpressionVar?.type ?: context.irBuiltIns.unitType
                }
        }

        val scriptTransformer = ReplSnippetToClassTransformer(
            context,
            irSnippet,
            irSnippetClass,
            irSnippetClassThisReceiver,
            typeRemapper,
            snippetAccessCallsGenerator,
            valsToFields
        )
        val lambdaPatcher = ScriptFixLambdasTransformer(irSnippetClass)

        irSnippetClass.declarations.transformInPlace {
            val rootContext =
                if (it is IrConstructor)
                    ScriptLikeToClassTransformerContext.makeRootContext(irSnippetClass.thisReceiver!!.symbol, true)
                else
                    ScriptLikeToClassTransformerContext.makeRootContext(null, isInScriptConstructor = false, topLevelDeclaration = it)
            it.transform(scriptTransformer, rootContext)
                .transform(lambdaPatcher, ScriptFixLambdasTransformerContext())
        }

        irSnippetClass.annotations += (irSnippetClass.parent as IrFile).annotations
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun createConstructor(irSnippetClass: IrClass): IrConstructor =
        with(IrFunctionBuilder().apply {
            isPrimary = true
            returnType = irSnippetClass.thisReceiver!!.type as IrSimpleType
        }) {
            irSnippetClass.factory.createConstructor(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = SpecialNames.INIT,
                visibility = visibility,
                isInline = isInline,
                isExpect = isExpect,
                returnType = returnType,
                symbol = IrConstructorSymbolImpl(),
                isPrimary = isPrimary,
                isExternal = isExternal,
                containerSource = containerSource,
            )
        }.also { irConstructor ->
            irConstructor.body = context.irBuiltIns.createIrBuilder(irConstructor.symbol).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
            }
            irConstructor.parent = irSnippetClass
        }
}

private class ReplSnippetsToClassesSymbolRemapper : SymbolRemapper.Empty() {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol =
        (symbol.owner as? IrReplSnippet)?.targetClass ?: symbol
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private class ReplSnippetAccessCallsGenerator(
    context: IrPluginContext,
    snippetClassReceiver: IrValueParameter,
    implicitReceiversFieldsWithParameters: ArrayList<Pair<IrField, IrValueParameter>>,
    val irSnippetClass: IrClass,
    val irReplStateObjectSymbol: IrClassSymbol
) : ScriptLikeAccessCallsGenerator(context, snippetClassReceiver, implicitReceiversFieldsWithParameters) {
    private val mapClass = irReplStateObjectSymbol.owner
    private val mapGet = mapClass.functions.single { it.name.asString() == "get" }
    private val mapPut = mapClass.functions.single { it.name.asString() == "put" }

    fun createIrGetValFromState(startOffset: Int, endOffset: Int, irSnippetClassFromState: IrClassSymbol, irTargetField: IrFieldSymbol, data: ScriptLikeToClassTransformerContext): IrExpression =
        IrGetFieldImpl(
            startOffset,
            endOffset,
            irTargetField,
            irTargetField.owner.type,
            IrStatementOrigin.GET_PROPERTY
        ).also {
            it.receiver =
                createAccessToSnippet(irSnippetClassFromState, data, startOffset, endOffset)
        }

    fun createIrSetValInState(startOffset: Int, endOffset: Int, irSnippetClassFromState: IrClassSymbol, irTargetField: IrFieldSymbol, expression: IrExpression, data: ScriptLikeToClassTransformerContext) : IrExpression =
        IrSetFieldImpl(
            startOffset,
            endOffset,
            irTargetField,
            context.irBuiltIns.unitType,
        ).also {
            it.receiver = createAccessToSnippet(irSnippetClassFromState, data, startOffset, endOffset)
            it.value = expression
        }

    fun createPutSelfToState(data: ScriptLikeToClassTransformerContext): IrCall =
        IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, mapPut.returnType, mapPut.symbol).apply {
            dispatchReceiver =
                IrGetObjectValueImpl(
                    startOffset, endOffset,
                    irReplStateObjectSymbol.typeWith(),
                    irReplStateObjectSymbol,
                )
            putValueArgument(
                0,
                IrConstImpl.string(
                    startOffset,
                    endOffset,
                    context.irBuiltIns.stringType,
                    irSnippetClass.name.asString()
                )
            )
            putValueArgument(1, getAccessCallForSelf(data, startOffset, endOffset, null, null))
        }

    fun createAccessToSnippet(
        irSnippetClassFromState: IrClassSymbol,
        data: ScriptLikeToClassTransformerContext,
        startOffset: Int,
        endOffset: Int,
    ): IrExpression? =
        if (irSnippetClassFromState == irSnippetClass.symbol)
            getAccessCallForSelf(data, startOffset, endOffset, IrStatementOrigin.GET_PROPERTY, null)
        else {
            val getSnippetCall = IrCallImpl(startOffset, endOffset, mapGet.returnType, mapGet.symbol).apply {
                dispatchReceiver =
                    IrGetObjectValueImpl(
                        startOffset, endOffset,
                        irReplStateObjectSymbol.typeWith(),
                        irReplStateObjectSymbol,
                    )
                putValueArgument(
                    0,
                    IrConstImpl.string(
                        startOffset,
                        endOffset,
                        context.irBuiltIns.stringType,
                        irSnippetClassFromState.owner.name.asString()
                    )
                )
            }
            val targetType = irSnippetClassFromState.typeWith()
            IrTypeOperatorCallImpl(
                startOffset, endOffset, targetType,
                IrTypeOperator.CAST,
                targetType, getSnippetCall
            )
        }

}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private class ReplSnippetToClassTransformer(
    context: IrPluginContext,
    val irSnippet: IrReplSnippet,
    irSnippetClass: IrClass,
    snippetClassReceiver: IrValueParameter,
    typeRemapper: TypeRemapper,
    override val accessCallsGenerator: ReplSnippetAccessCallsGenerator,
    val varsToFields: Map<IrVariableSymbol, IrFieldSymbol>,
) : ScriptLikeToClassTransformer(
    context,
    irSnippet,
    irSnippetClass,
    snippetClassReceiver,
    typeRemapper,
    accessCallsGenerator,
    // currently assuming that the snippet top level decls do not capture anything from the snippet or history, but use global state instead
    capturingClasses = emptySet(),
    needsReceiverProcessing = true
) {
    override fun visitSimpleFunction(
        declaration: IrSimpleFunction,
        data: ScriptLikeToClassTransformerContext,
    ): IrSimpleFunction {
        if (declaration.parent == irSnippet || declaration.parent == irTargetClass) {
            declaration.visibility = DescriptorVisibilities.PUBLIC
        }
        return super.visitSimpleFunction(declaration, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: ScriptLikeToClassTransformerContext): IrExpression {
        val targetFieldInThisSnippet = varsToFields[expression.symbol]
        return if (targetFieldInThisSnippet != null) {
            accessCallsGenerator.createIrGetValFromState(
                expression.startOffset, expression.endOffset,
                irTargetClass.symbol,
                targetFieldInThisSnippet,
                data
            ).also { it.transformChildren(this, data) }
        } else {
            val targetFieldFromOtherSnippets =
                irSnippet.variablesFromOtherSnippets.find {
                    it.name == expression.symbol.owner.name
                }?.originalSnippetValueSymbol as? IrFieldSymbol
            if (targetFieldFromOtherSnippets != null) {
                accessCallsGenerator.createIrGetValFromState(
                    expression.startOffset, expression.endOffset,
                    targetFieldFromOtherSnippets.owner.parentAsClass.symbol,
                    targetFieldFromOtherSnippets,
                    data
                ).also { it.transformChildren(this, data) }
            } else {
                super.visitGetValue(expression, data)
            }
        }
    }

    override fun visitSetValue(
        expression: IrSetValue,
        data: ScriptLikeToClassTransformerContext,
    ): IrExpression {
        val targetFieldInThisSnippet = varsToFields[expression.symbol]
        return if (targetFieldInThisSnippet != null) {
            accessCallsGenerator.createIrSetValInState(
                expression.startOffset, expression.endOffset,
                irTargetClass.symbol,
                targetFieldInThisSnippet,
                expression.value,
                data
            ).also { it.transformChildren(this, data) }
        } else {
            val targetFieldFromOtherSnippets =
                irSnippet.variablesFromOtherSnippets.find {
                    it.name == expression.symbol.owner.name
                }?.originalSnippetValueSymbol as? IrFieldSymbol
            if (targetFieldFromOtherSnippets != null) {
                accessCallsGenerator.createIrSetValInState(
                    expression.startOffset, expression.endOffset,
                    targetFieldFromOtherSnippets.owner.parentAsClass.symbol,
                    targetFieldFromOtherSnippets,
                    expression.value,
                    data
                ).also { it.transformChildren(this, data) }
            } else {
                super.visitSetValue(expression, data)
            }
        }
    }

    override fun visitCall(expression: IrCall, data: ScriptLikeToClassTransformerContext): IrElement {
        return if (expression.symbol.owner in irSnippet.capturingDeclarationsFromOtherSnippets) {
            expression.insertDispatchReceiver(
                accessCallsGenerator.createAccessToSnippet(
                    (expression.symbol.owner.parent as IrClass).symbol,
                    data, expression.startOffset, expression.endOffset
                )
            )
            expression.transformChildren(this, data)
            expression
        } else {
            super.visitCall(expression, data)
        }
    }

    override fun visitConstructorCall(
        expression: IrConstructorCall,
        data: ScriptLikeToClassTransformerContext,
    ): IrExpression {
        return if ((expression.symbol.owner.parent as? IrDeclaration)?.let { it in irSnippet.capturingDeclarationsFromOtherSnippets } == true) {
            expression.arguments +=
                accessCallsGenerator.createAccessToSnippet(
                    ((expression.symbol.owner.parent as IrClass).parent as IrClass).symbol,
                    data, expression.startOffset, expression.endOffset
                )
            expression.transformChildren(this, data)
            expression
        } else {
            super.visitConstructorCall(expression, data)
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun makeImplicitReceiversFieldsWithParameters(irSnippetClass: IrClass, typeRemapper: SimpleTypeRemapper, irSnippet: IrReplSnippet) =
    arrayListOf<Pair<IrField, IrValueParameter>>().apply {

        fun createField(name: Name, type: IrType): IrField {
            val field = irSnippetClass.factory.createField(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.SCRIPT_IMPLICIT_RECEIVER,
                name = name,
                visibility = DescriptorVisibilities.PRIVATE,
                symbol = IrFieldSymbolImpl(),
                type = typeRemapper.remapType(type),
                isFinal = true,
                isStatic = false,
                isExternal = false
            )
            field.parent = irSnippetClass
            irSnippetClass.declarations.add(field)
            return field
        }

        irSnippet.receiversParameters.forEach { param ->
            val typeName = param.type.classFqName?.shortName()?.identifierOrNullIfSpecial
            add(
                createField(
                    Name.identifier($$$"$$implicitReceiver_$$${typeName ?: param.indexInParameters.toString()}"),
                    param.type
                ) to param
            )
        }
    }
