/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.getOrPut
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.ir.util.getAnnotationStringValue
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class FunctionsWithoutBoundsCheckSupport(
        private val mapping: NativeMapping,
        symbols: KonanSymbols,
        private val irBuiltIns: IrBuiltIns,
        private val irFactory: IrFactory,
) {
    private val list = symbols.list.owner

    fun IrFunction.isGet() =
            name == OperatorNameConventions.GET && dispatchReceiverParameter != null
                    && valueParameters.singleOrNull()?.type == irBuiltIns.intType

    fun IrClass.getGet() = functions.single { it.isGet() }

    fun IrClass.needFunctionsWithoutBoundsCheck() =
            this == list || list in this.getAllSuperclasses()

    fun IrClass.getGetWithoutBoundsCheck(): IrSimpleFunction = mapping.listGetWithoutBoundsCheck.getOrPut(this) {
        val get = this.getGet()
        irFactory.buildFun {
            name = KonanNameConventions.getWithoutBoundCheck
            modality = get.modality
            returnType = get.returnType
            isFakeOverride = get.isFakeOverride
        }.apply {
            parent = this@getGetWithoutBoundsCheck
            createDispatchReceiverParameter()
            addValueParameter {
                name = Name.identifier("index")
                type = irBuiltIns.intType
            }

            overriddenSymbols = get.overriddenSymbols.map { it.owner.parentAsClass.getGetWithoutBoundsCheck().symbol }

            val parentDeclarations = this@getGetWithoutBoundsCheck.declarations
            parentDeclarations.add(parentDeclarations.indexOf(get) + 1, this)
        }
    }
}

internal class BoundsCheckOptimizer(val context: Context) : BodyLoweringPass {
    private val symbols = context.ir.symbols
    private val noBoundsCheck = symbols.noBoundsCheck.owner
    private val arrays = symbols.arrays.toSet()
    private val functionsWithoutBoundsCheckSupport = context.functionsWithoutBoundsCheckSupport

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val irBuilder = context.createIrBuilder(container.symbol)
        irBody.transformChildren(object : IrElementTransformer<Boolean> {
            override fun visitBlock(expression: IrBlock, data: Boolean) =
                    super.visitBlock(expression, data || (expression as? IrReturnableBlock)?.inlineFunction?.originalFunction == noBoundsCheck)

            override fun visitCall(expression: IrCall, data: Boolean): IrElement {
                expression.transformChildren(this, data)

                if (data) {
                    val callee = expression.symbol.owner
                    with(functionsWithoutBoundsCheckSupport) {
                        if (callee.isGet()) {
                            val irClass = callee.parentAsClass
                            val getWithoutBoundsCheck = when {
                                irClass.symbol in arrays ->
                                    irClass.functions.single { it.name == KonanNameConventions.getWithoutBoundCheck }
                                irClass.needFunctionsWithoutBoundsCheck() ->
                                    irClass.getGetWithoutBoundsCheck()
                                else -> null
                            }
                            if (getWithoutBoundsCheck != null) {
                                return irBuilder.at(expression)
                                        .irCall(getWithoutBoundsCheck, superQualifierSymbol = expression.superQualifierSymbol).apply {
                                            dispatchReceiver = expression.dispatchReceiver
                                            putValueArgument(0, expression.getValueArgument(0))
                                        }
                            }
                        }
                    }
                }

                return expression
            }
        }, data = false)
    }

}

internal class ListAccessorsWithoutBoundsCheckLowering(val context: Context) : ClassLoweringPass {
    private val irBuiltIns = context.irBuiltIns
    private val symbols = context.ir.symbols
    private val functionsWithoutBoundsCheckSupport = context.functionsWithoutBoundsCheckSupport

    private val noBoundsCheck = symbols.noBoundsCheck.owner
    private val abstractList = symbols.abstractList.owner

    private val checkElementIndex = abstractList.companionObject()!!.functions.single {
        it.name.asString() == "checkElementIndex"
                && it.valueParameters.size == 2
                && it.valueParameters.all { parameter -> parameter.type == irBuiltIns.intType }
    }

    override fun lower(irClass: IrClass) = with(functionsWithoutBoundsCheckSupport) {
        if (irClass.needFunctionsWithoutBoundsCheck()) {
            val get = irClass.getGet()
            val getWithoutBoundsCheck = irClass.getGetWithoutBoundsCheck()
            if (getWithoutBoundsCheck.modality != Modality.ABSTRACT && !getWithoutBoundsCheck.isFakeOverride) {
                val irBuilder = context.createIrBuilder(getWithoutBoundsCheck.symbol)
                val body = get.body
                        .takeIf { (irClass.packageFqName?.asString()?.startsWith("kotlin.") == true) }
                        ?.deepCopyWithVariables()
                        ?.setDeclarationsParent(getWithoutBoundsCheck)
                body?.transformGetBody(irBuilder, get, getWithoutBoundsCheck)
                getWithoutBoundsCheck.body =
                        body ?: irBuilder.run {
                            // Unknown List inheritor: just conservatively delegate to get.
                            irBlockBody {
                                +irReturn(
                                        irCall(get, superQualifierSymbol = irClass.symbol).apply {
                                            dispatchReceiver = irGet(getWithoutBoundsCheck.dispatchReceiverParameter!!)
                                            putValueArgument(0, irGet(getWithoutBoundsCheck.valueParameters[0]))
                                        }
                                )
                            }
                        }
            }
        }
    }

    private fun IrBody.transformGetBody(irBuilder: DeclarationIrBuilder, get: IrFunction, getWithoutBoundsCheck: IrFunction) {
        transformChildren(object : IrElementTransformer<Boolean> {
            override fun visitReturn(expression: IrReturn, data: Boolean): IrExpression {
                if (expression.returnTargetSymbol != get.symbol)
                    return super.visitReturn(expression, data)

                expression.transformChildren(this, data)

                return if (data)
                    irBuilder.at(expression).irReturn(expression.value)
                else {
                    val value = expression.value
                    val returnableBlockSymbol = IrReturnableBlockSymbolImpl()
                    val noBoundsCheckInlinedBlock = IrInlinedFunctionBlockImpl(
                            startOffset = value.startOffset,
                            endOffset = value.endOffset,
                            type = value.type,
                            // This call has no arguments, which is fine since it's not used in K/N.
                            inlineCall = irBuilder.irCall(noBoundsCheck.symbol, noBoundsCheck.returnType, listOf(value.type)),
                            inlinedElement = noBoundsCheck,
                            origin = null,
                            statements = listOf(
                                    IrReturnImpl(
                                            value.startOffset, value.endOffset,
                                            irBuiltIns.nothingType,
                                            returnableBlockSymbol,
                                            value
                                    )
                            )
                    )
                    irBuilder.at(expression).irReturn(
                            IrReturnableBlockImpl(
                                    startOffset = value.startOffset,
                                    endOffset = value.endOffset,
                                    type = value.type,
                                    symbol = returnableBlockSymbol,
                                    origin = null,
                                    statements = listOf(noBoundsCheckInlinedBlock)
                            )
                    )
                }
            }

            override fun visitGetValue(expression: IrGetValue, data: Boolean): IrExpression {
                expression.transformChildren(this, data)

                return when (val value = expression.symbol.owner) {
                    get.dispatchReceiverParameter -> irBuilder.at(expression).irGet(getWithoutBoundsCheck.dispatchReceiverParameter!!)
                    is IrValueParameter -> irBuilder.at(expression).irGet(getWithoutBoundsCheck.valueParameters[value.index])
                    else -> expression
                }
            }

            override fun visitBlock(expression: IrBlock, data: Boolean) =
                    super.visitBlock(expression, data || (expression as? IrReturnableBlock)?.inlineFunction == noBoundsCheck)

            override fun visitCall(expression: IrCall, data: Boolean) = when (expression.symbol) {
                // Skip bounds check.
                checkElementIndex.symbol -> IrCompositeImpl(expression.startOffset, expression.endOffset, irBuiltIns.unitType)
                else -> super.visitCall(expression, data)
            }
        }, data = false)
    }
}

// Generate additional functions for array set and get operators without bounds checking.
internal class FunctionsWithoutBoundCheckGenerator(val context: KonanBackendContext) {
    private val symbols = context.ir.symbols

    private fun generateFunction(baseFunction: IrSimpleFunction, delegatingToFunction: IrSimpleFunction?, functionName: Name) =
            context.irFactory.createSimpleFunction(
                    startOffset = baseFunction.startOffset,
                    endOffset = baseFunction.endOffset,
                    origin = IrDeclarationOrigin.GENERATED_SETTER_GETTER,
                    name = functionName,
                    visibility = DescriptorVisibilities.PUBLIC,
                    isInline = false,
                    isExpect = false,
                    returnType = baseFunction.returnType,
                    modality = Modality.FINAL,
                    symbol = IrSimpleFunctionSymbolImpl(),
                    isTailrec = false,
                    isSuspend = false,
                    isOperator = false,
                    isInfix = false,
                    isExternal = true,
            ).also { function ->
                function.parent = baseFunction.parent
                function.createDispatchReceiverParameter()
                function.valueParameters = baseFunction.valueParameters.map { it.copyTo(function) }
                // Copy annotations.
                val setWithoutBEAnnotations = (delegatingToFunction ?: baseFunction).annotations.map { annotation ->
                    annotation.deepCopyWithSymbols().also { copy ->
                        if (copy.isAnnotationWithEqualFqName(KonanFqNames.gcUnsafeCall)) {
                            val value = "${annotation.getAnnotationStringValue("callee")}_without_BoundCheck"
                            copy.putValueArgument(0,
                                    IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.stringType, value))
                        }
                    }
                }
                function.annotations = setWithoutBEAnnotations
            }

    fun generate() {
        symbols.arrays.forEach { classSymbol ->
            val underlyingClass = (classSymbol.defaultType.computeBinaryType() as BinaryType.Reference)
                    .types.single().takeIf { classSymbol.owner.isSingleFieldValueClass }
            val setFunction = classSymbol.owner.functions.single { it.name == OperatorNameConventions.SET }
            val setDelegatingToFunction = underlyingClass?.functions?.single { it.name == OperatorNameConventions.SET }
            classSymbol.owner.addMember(generateFunction(setFunction, setDelegatingToFunction, KonanNameConventions.setWithoutBoundCheck))

            val getFunction = classSymbol.owner.functions.single { it.name == OperatorNameConventions.GET }
            val getDelegatingToFunction = underlyingClass?.functions?.single { it.name == OperatorNameConventions.GET }
            classSymbol.owner.addMember(generateFunction(getFunction, getDelegatingToFunction, KonanNameConventions.getWithoutBoundCheck))
        }
    }
}