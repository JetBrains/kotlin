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
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
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
    private val mutableList = symbols.mutableList.owner

    fun IrFunction.isGet() =
            name == OperatorNameConventions.GET && dispatchReceiverParameter != null
                    && valueParameters.singleOrNull()?.type == irBuiltIns.intType

    fun IrFunction.isSet() =
            name == OperatorNameConventions.SET && dispatchReceiverParameter != null
                    && valueParameters.size == 2
                    && valueParameters[0].type == irBuiltIns.intType

    fun IrClass.getGet() = functions.single { it.isGet() }
    fun IrClass.getSet() = functions.single { it.isSet() }

    fun IrClass.needGetWithoutBoundsCheck() =
            this == list || list in this.getAllSuperclasses()

    fun IrClass.needSetWithoutBoundsCheck() =
            this == mutableList || mutableList in this.getAllSuperclasses()

    fun IrClass.getGetWithoutBoundsCheck(): IrSimpleFunction = mapping.listGetWithoutBoundsCheck.getOrPut(this) {
        val get = this.getGet()
        buildFunctionWithoutBoundsCheck(
                get,
                KonanNameConventions.getWithoutBoundCheck,
                get.overriddenSymbols.map { it.owner.parentAsClass.getGetWithoutBoundsCheck().symbol }
        )
    }

    fun IrClass.getSetWithoutBoundsCheck(): IrSimpleFunction = mapping.listSetWithoutBoundsCheck.getOrPut(this) {
        val set = this.getSet()
        buildFunctionWithoutBoundsCheck(
                set,
                KonanNameConventions.setWithoutBoundCheck,
                set.overriddenSymbols.map { it.owner.parentAsClass.getSetWithoutBoundsCheck().symbol }
        )
    }

    private fun IrClass.buildFunctionWithoutBoundsCheck(
            function: IrSimpleFunction,
            name: Name,
            overriddenSymbols: List<IrSimpleFunctionSymbol>
    ) = irFactory.buildFun {
        this.name = name
        modality = function.modality
        returnType = function.returnType
        isFakeOverride = function.isFakeOverride
    }.apply {
        parent = this@buildFunctionWithoutBoundsCheck
        createDispatchReceiverParameter()

        function.valueParameters.forEach {
            addValueParameter {
                this.name = it.name
                type = it.type
            }
        }

        this.overriddenSymbols = overriddenSymbols

        val parentDeclarations = this@buildFunctionWithoutBoundsCheck.declarations
        parentDeclarations.add(parentDeclarations.indexOf(function) + 1, this)
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
                        val functionWithoutBoundsCheck = when {
                            callee.isGet() -> {
                                val irClass = callee.parentAsClass
                                when {
                                    irClass.symbol in arrays ->
                                        irClass.functions.single { it.name == KonanNameConventions.getWithoutBoundCheck }
                                    irClass.needGetWithoutBoundsCheck() ->
                                        irClass.getGetWithoutBoundsCheck()
                                    else -> null
                                }
                            }
                            callee.isSet() -> {
                                val irClass = callee.parentAsClass
                                when {
                                    irClass.symbol in arrays ->
                                        irClass.functions.single { it.name == KonanNameConventions.setWithoutBoundCheck }
                                    irClass.needSetWithoutBoundsCheck() ->
                                        irClass.getSetWithoutBoundsCheck()
                                    else -> null
                                }
                            }
                            else -> null
                        }

                        if (functionWithoutBoundsCheck != null) {
                            return irBuilder.at(expression)
                                    .irCall(functionWithoutBoundsCheck, superQualifierSymbol = expression.superQualifierSymbol).apply {
                                        dispatchReceiver = expression.dispatchReceiver
                                        (0..<expression.valueArgumentsCount).forEach {
                                            putValueArgument(it, expression.getValueArgument(it))
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
//        if (irClass.name.asString() == "EmptyList") {
//            println("BEFORE: ${System.identityHashCode(irClass)} ${irClass.dump()}")
//        }
        if (irClass.needGetWithoutBoundsCheck()) {
//            if (irClass.name.asString() == "EmptyList") {
//                println("YEAH, BABY!")
//            }
            val get = irClass.getGet()
            val getWithoutBoundsCheck = irClass.getGetWithoutBoundsCheck()
            buildFunctionWithoutBoundsCheckBody(irClass, get, getWithoutBoundsCheck)
        }
        if (irClass.needSetWithoutBoundsCheck()) {
            val set = irClass.getSet()
            val setWithoutBoundsCheck = irClass.getSetWithoutBoundsCheck()
            buildFunctionWithoutBoundsCheckBody(irClass, set, setWithoutBoundsCheck)
        }
//        if (irClass.name.asString() == "EmptyList") {
//            println("AFTER: ${System.identityHashCode(irClass)} ${irClass.dump()}")
//        }
    }

    private fun buildFunctionWithoutBoundsCheckBody(
            irClass: IrClass,
            function: IrSimpleFunction,
            functionWithoutBoundsCheck: IrSimpleFunction
    ) {
        if (functionWithoutBoundsCheck.modality == Modality.ABSTRACT || functionWithoutBoundsCheck.isFakeOverride)
            return

        val irBuilder = context.createIrBuilder(functionWithoutBoundsCheck.symbol)
        val body = function.body
                .takeIf { (irClass.packageFqName?.asString()?.startsWith("kotlin.") == true) }
                ?.deepCopyWithVariables()
                ?.setDeclarationsParent(functionWithoutBoundsCheck)
        body?.transformGetBody(irBuilder, function, functionWithoutBoundsCheck)
        functionWithoutBoundsCheck.body =
                body ?: irBuilder.run {
                    // Unknown List inheritor: just conservatively delegate to get.
                    irBlockBody {
                        +irReturn(
                                irCall(function, superQualifierSymbol = irClass.symbol).apply {
                                    dispatchReceiver = irGet(functionWithoutBoundsCheck.dispatchReceiverParameter!!)
                                    for (parameter in functionWithoutBoundsCheck.valueParameters)
                                        putValueArgument(parameter.index, irGet(parameter))
                                }
                        )
                    }
                }
    }

    private fun IrBody.transformGetBody(irBuilder: DeclarationIrBuilder, function: IrFunction, functionWithoutBoundsCheck: IrFunction) {
        transformChildren(object : IrElementTransformer<Boolean> {
            override fun visitReturn(expression: IrReturn, data: Boolean): IrExpression {
                if (expression.returnTargetSymbol != function.symbol)
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
                    function.dispatchReceiverParameter -> irBuilder.at(expression).irGet(functionWithoutBoundsCheck.dispatchReceiverParameter!!)
                    is IrValueParameter -> irBuilder.at(expression).irGet(functionWithoutBoundsCheck.valueParameters[value.index])
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