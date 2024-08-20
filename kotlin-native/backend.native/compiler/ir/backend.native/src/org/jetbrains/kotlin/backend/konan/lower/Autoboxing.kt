/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.AbstractValueUsageTransformer
import org.jetbrains.kotlin.utils.atMostOne
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.cgen.hasCCallAnnotation
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstantPrimitiveImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.objcinterop.isObjCForwardDeclaration
import org.jetbrains.kotlin.ir.objcinterop.isObjCMetaClass
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull

/**
 * Boxes and unboxes values of value types when necessary.
 */
internal class Autoboxing(val context: Context) : FileLoweringPass {

    private val transformer = AutoboxingTransformer(context)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(transformer)
        irFile.transform(InlineClassTransformer(context), data = null)
    }

}

private class AutoboxingTransformer(val context: Context) : AbstractValueUsageTransformer(
        context.ir.symbols,
        context.irBuiltIns
) {
    private val insertSafeCasts = context.config.genericSafeCasts

    // TODO: should we handle the cases when expression type
    // is not equal to e.g. called function return type?

    override fun IrExpression.useInTypeOperator(operator: IrTypeOperator, typeOperand: IrType) = when {
        operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT || operator == IrTypeOperator.IMPLICIT_INTEGER_COERCION -> this
        insertSafeCasts && operator == IrTypeOperator.IMPLICIT_CAST -> {
            if (typeOperand.isInlinedNative())
                this.useAs(context.irBuiltIns.anyNType)
                        .useAs(typeOperand)
            else
                this.useAs(typeOperand)
        }
        else -> {
            // Codegen expects the argument of type-checking operator to be an object reference:
            this.useAs(context.irBuiltIns.anyNType)
        }
    }

    private var currentFunction: IrFunction? = null
    private val irBuilders = mutableListOf<DeclarationIrBuilder>()

    override fun visitField(declaration: IrField): IrStatement {
        irBuilders.push(context.createIrBuilder(declaration.symbol))
        val result = super.visitField(declaration)
        irBuilders.pop()
        return result
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        currentFunction = declaration
        irBuilders.push(context.createIrBuilder(declaration.symbol))
        val result = super.visitFunction(declaration)
        irBuilders.pop()
        currentFunction = null
        return result
    }

    override fun IrExpression.useAsReturnValue(returnTarget: IrReturnTargetSymbol): IrExpression = when (returnTarget) {
        is IrSimpleFunctionSymbol -> this.useAs(returnTarget.owner.returnType)
        is IrConstructorSymbol -> this.useAs(irBuiltIns.unitType)
        is IrReturnableBlockSymbol -> this.useAs(returnTarget.owner.type)
    }

    override fun IrExpression.useAs(type: IrType): IrExpression {
        return this.useAs(type, forceSkipTypeCheck = false)
    }

    private fun IrExpression.useAs(type: IrType, forceSkipTypeCheck: Boolean): IrExpression {
        val skipTypeCheck = forceSkipTypeCheck || !insertSafeCasts || (this as? IrTypeOperatorCall)?.operator == IrTypeOperator.CAST
        val actualType = when (this) {
            is IrGetField -> this.symbol.owner.type
            is IrCall -> when (this.symbol) {
                symbols.reinterpret -> this.getTypeArgument(1)!!
                else -> this.callTarget.returnType
            }
            is IrTypeOperatorCall -> when (this.operator) {
                IrTypeOperator.CAST -> context.irBuiltIns.anyNType
                IrTypeOperator.IMPLICIT_CAST -> if (insertSafeCasts) this.type else context.irBuiltIns.anyNType
                else -> this.type
            }
            else -> this.type
        }
        return if (this.type.isUnit() && !actualType.isUnit())
            irBuilders.peek()!!.at(this).irImplicitCoercionToUnit(this)
                    .adaptIfNecessary(actualType = irBuiltIns.unitType, type, skipTypeCheck)
        else
            this.adaptIfNecessary(actualType, type, skipTypeCheck)
    }

    private val IrFunctionAccessExpression.target: IrFunction
        get() = when (this) {
            is IrCall -> this.callTarget
            is IrDelegatingConstructorCall -> this.symbol.owner
            is IrConstructorCall -> this.symbol.owner
            is IrEnumConstructorCall -> compilationException("IrEnumConstructorCall is not supported here", this)
        }

    private val IrCall.callTarget: IrFunction
        get() = if (this.isVirtualCall) {
            symbol.owner
        } else {
            symbol.owner.target
        }

    override fun IrExpression.useAsDispatchReceiver(expression: IrFunctionAccessExpression): IrExpression {
        val target = expression.target
        return useAs(target.dispatchReceiverParameter!!.type,
                // A bridge cannot be called on an improper receiver.
                forceSkipTypeCheck = (currentFunction as? IrSimpleFunction)?.bridgeTarget == target)
    }

    override fun IrExpression.useAsExtensionReceiver(expression: IrFunctionAccessExpression): IrExpression {
        return this.useAsArgument(expression.target.extensionReceiverParameter!!)
    }

    override fun IrExpression.useAsValueArgument(expression: IrFunctionAccessExpression,
                                                 parameter: IrValueParameter): IrExpression {
        return this.useAsArgument(expression.target.valueParameters[parameter.index])
    }

    /**
     * Performs an actual type check operation.
     */
    private fun IrExpression.checkedCast(actualType: IrType, expectedType: IrType) =
            irBuilders.peek()!!.at(this).run {
                val expression = irImplicitCast(this@checkedCast, actualType)
                if (expectedType.isNullable())
                    irAs(expression, expectedType)
                else irAs(expression, expectedType.makeNullable())
            }

    private fun IrClass.canBeAssignedTo(expectedClass: IrClass) =
            this.isNothing() || this.symbol.isSubtypeOfClass(expectedClass.symbol)

    private fun IrExpression.adaptIfNecessary(actualType: IrType, expectedType: IrType, skipTypeCheck: Boolean = false): IrExpression {
        val conversion = context.getTypeConversion(actualType, expectedType)
        return if (conversion == null) {
            val actualClass = actualType.classOrNull?.owner
            val erasedExpectedType = expectedType.erasure()
            val erasedExpectedClass = erasedExpectedType.classOrFail.owner
            return when {
                actualType.makeNotNull() == expectedType.makeNotNull() -> this
                expectedType.isUnit() ->
                    irBuilders.peek()!!.at(this).irImplicitCoercionToUnit(this)
                insertSafeCasts && !skipTypeCheck
                        // For type parameters, actualClass is null, and we
                        // conservatively insert type check for them (due to unsafe casts).
                        && actualClass?.canBeAssignedTo(erasedExpectedType.getClass()!!) != true
                        && actualType.getInlinedClassNative() == null
                        && !erasedExpectedClass.isObjCForwardDeclaration()
                        && !erasedExpectedClass.isObjCMetaClass() // See KT-65260 for details.
                -> {
                    this.checkedCast(actualType, erasedExpectedType)
                }
                else -> this
            }
        } else {
            when (this) {
                is IrConst -> IrConstantPrimitiveImpl(this.startOffset, this.endOffset, this)
                is IrConstantPrimitive, is IrConstantObject -> this
                is IrConstantValue -> TODO("Boxing/unboxing of ${this::class.qualifiedName} is not supported")
                else -> null
            }?.let {
                it.type = expectedType
                return it
            }
            val parameter = conversion.owner.valueParameters.single()
            val argument = if (insertSafeCasts && !skipTypeCheck && expectedType.isInlinedNative())
                this.checkedCast(actualType, conversion.owner.returnType)
            else this

            irBuilders.peek()!!.at(this)
                    .irCall(conversion).apply { this.putValueArgument(parameter.index, argument) }
        }
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        expression.transformChildrenVoid()
        assert(expression.getArgumentsWithIr().isEmpty())
        return expression
    }

    override fun visitCall(expression: IrCall): IrExpression {
        return when (expression.symbol) {
            symbols.reinterpret -> {
                expression.transformChildrenVoid()

                // TODO: check types has the same binary representation.
                val oldType = expression.getTypeArgument(0)!!
                val newType = expression.getTypeArgument(1)!!

                assert(oldType.computePrimitiveBinaryTypeOrNull() == newType.computePrimitiveBinaryTypeOrNull())

                expression.extensionReceiver = expression.extensionReceiver!!.useAs(oldType)

                expression
            }

            else -> super.visitCall(expression)
        }
    }

}

private class InlineClassTransformer(private val context: Context) : IrBuildingTransformer(context) {

    private val symbols = context.ir.symbols
    private val irBuiltIns = context.irBuiltIns

    private val builtBoxUnboxFunctions = mutableListOf<IrFunction>()

    override fun visitFile(declaration: IrFile): IrFile {
        declaration.transformChildrenVoid(this)
        declaration.declarations.addAll(builtBoxUnboxFunctions)
        builtBoxUnboxFunctions.clear()
        return declaration
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        super.visitClass(declaration)

        if (declaration.isInlined()) {
            if (declaration.isUsedAsBoxClass()) {
                if (declaration.isNativePrimitiveType()) {
                    buildBoxField(declaration)
                }

                buildBoxFunction(declaration, context.getBoxFunction(declaration))
                buildUnboxFunction(declaration, context.getUnboxFunction(declaration))
            }

            if (declaration.isNativePrimitiveType()) {
                // Constructors for these types aren't used and actually are malformed (e.g. lack the parameter).
                // Skipping here for simplicity.
            } else {
                declaration.constructors.toList().mapTo(declaration.declarations) {
                    context.getLoweredInlineClassConstructor(it)
                }
            }
        }

        return declaration
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        super.visitGetField(expression)

        val field = expression.symbol.owner
        val parentClass = field.parentClassOrNull
        return if (parentClass == null || !parentClass.isInlined() || field.isStatic)
            expression
        else {
            builder.at(expression)
                    .irCall(symbols.reinterpret, field.type,
                            listOf(parentClass.defaultType, field.type)
                    ).apply {
                        extensionReceiver = expression.receiver!!
                    }
        }
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        super.visitSetField(expression)

        return if (expression.symbol.owner.parentClassOrNull?.isInlined() == true && !expression.symbol.owner.isStatic) {
            // Happens in one of the cases:
            // 1. In primary constructor of the inlined class. Makes no sense, "has no effect", can be removed.
            //    The constructor will be lowered and used.
            // 2. In setter of NativePointed.rawPtr. It is generally a hack and isn't actually used.
            //    TODO: it is better to get rid of it.
            //
            // So drop the entire IrSetField:
            return builder.irComposite(expression) {}
        } else {
            expression
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        super.visitConstructorCall(expression)

        val constructor = expression.symbol.owner
        return if (constructor.constructedClass.isInlined()) {
            builder.lowerConstructorCallToValue(expression, constructor)
        } else {
            expression
        }
    }

    override fun visitConstructor(declaration: IrConstructor): IrStatement {
        super.visitConstructor(declaration)

        if (declaration.constructedClass.isInlined()) {
            if (declaration.constructedClass.isNativePrimitiveType()) {
                // Constructors for these types aren't used and actually are malformed (e.g. lack the parameter).
                // Skipping here for simplicity.
            } else if (declaration.hasCCallAnnotation("CppClassConstructor") && !declaration.isPrimary) {
                // At this point secondary cpp constructor calls have already been transformed
                // by interop lowering. So don't mess with them.
                // Otherwise we could assert having assumptions on (empty at the moment) body of the constructor.
            } else {
                buildLoweredConstructor(declaration)
            }
            // TODO: fix DFG building and nullify the body instead.
            (declaration.body as IrBlockBody).statements.clear()
        }

        return declaration
    }

    private fun IrBuilderWithScope.irIsNull(expression: IrExpression): IrExpression {
        val binary = expression.type.computeBinaryType()
        return when (binary) {
            is BinaryType.Primitive -> {
                assert(binary.type == PrimitiveBinaryType.POINTER)
                irCall(symbols.areEqualByValue[binary.type]!!.owner).apply {
                    putValueArgument(0, expression)
                    putValueArgument(1, irNullPointer())
                }
            }
            is BinaryType.Reference -> irCall(context.irBuiltIns.eqeqeqSymbol).apply {
                putValueArgument(0, expression)
                putValueArgument(1, irNull())
            }
        }
    }

    private fun buildBoxFunction(irClass: IrClass, function: IrFunction) {
        val builder = context.createIrBuilder(function.symbol)
        val cache = BoxCache.values().toList().atMostOne { context.irBuiltIns.getKotlinClass(it) == irClass }

        function.body = builder.irBlockBody(function) {
            val valueToBox = function.valueParameters[0]
            if (valueToBox.type.isNullable()) {
                +irIfThen(
                        condition = irIsNull(irGet(valueToBox)),
                        thenPart = irReturn(irNull())
                )
            }

            if (cache != null) {
                +irIfThen(
                        condition = irCall(symbols.boxCachePredicates[cache]!!.owner).apply {
                            putValueArgument(0, irGet(valueToBox))
                        },
                        thenPart = irReturn(irCall(symbols.boxCacheGetters[cache]!!.owner).apply {
                            putValueArgument(0, irGet(valueToBox))
                        })
                )
            }

            // Note: IR variable created below has reference type intentionally.
            val box = irTemporary(irCall(symbols.createUninitializedInstance.owner).also {
                it.putTypeArgument(0, irClass.defaultType)
            })
            +irSetField(irGet(box), getInlineClassBackingField(irClass), irGet(valueToBox))
            +irReturn(irGet(box))
        }

        builtBoxUnboxFunctions += function
    }

    private fun IrBuilderWithScope.irNullPointerOrReference(type: IrType): IrExpression =
            if (type.binaryTypeIsReference()) {
                irNull()
            } else {
                irNullPointer()
            }

    private fun IrBuilderWithScope.irNullPointer(): IrExpression = irCall(symbols.getNativeNullPtr.owner)

    private fun buildUnboxFunction(irClass: IrClass, function: IrFunction) {
        val builder = context.createIrBuilder(function.symbol)

        function.body = builder.irBlockBody(function) {
            val boxParameter = function.valueParameters.single()
            if (boxParameter.type.isNullable()) {
                +irIfThen(
                        condition = irEqeqeq(irGet(boxParameter), irNull()),
                        thenPart = irReturn(irNullPointerOrReference(function.returnType))
                )
            }
            +irReturn(irGetField(irGet(boxParameter), getInlineClassBackingField(irClass)))
        }

        builtBoxUnboxFunctions += function
    }

    private fun buildBoxField(declaration: IrClass) {
        val startOffset = declaration.startOffset
        val endOffset = declaration.endOffset

        val irField = context.irFactory.createField(
                startOffset,
                endOffset,
                IrDeclarationOrigin.DEFINED,
                Name.identifier("value"),
                DescriptorVisibilities.PRIVATE,
                IrFieldSymbolImpl(),
                declaration.defaultType,
                isFinal = true,
                isStatic = false,
        )
        irField.parent = declaration

        val irProperty = context.irFactory.createProperty(
                startOffset,
                endOffset,
                IrDeclarationOrigin.DEFINED,
                irField.name,
                irField.visibility,
                Modality.FINAL,
                IrPropertySymbolImpl(),
                isVar = false,
                isConst = false,
                isLateinit = false,
                isDelegated = false,
        )
        irProperty.backingField = irField

        declaration.addChild(irProperty)
    }

    private fun IrBuilderWithScope.lowerConstructorCallToValue(
            expression: IrMemberAccessExpression<*>,
            callee: IrConstructor
    ): IrExpression {
        this.at(expression)
        val loweredConstructor = this@InlineClassTransformer.context.getLoweredInlineClassConstructor(callee)
        return if (callee.isPrimary) this.irBlock {
            val argument = irTemporary(expression.getValueArgument(0)!!, irType = loweredConstructor.valueParameters.single().type)
            +irCall(loweredConstructor).apply {
                putValueArgument(0, irGet(argument))
            }
            +irGet(argument)
        } else this.irCall(loweredConstructor).apply {
            (0 until expression.valueArgumentsCount).forEach {
                putValueArgument(it, expression.getValueArgument(it)!!)
            }
        }
    }

    private fun buildLoweredConstructor(irConstructor: IrConstructor) {
        val result = context.getLoweredInlineClassConstructor(irConstructor)
        val irClass = irConstructor.parentAsClass

        result.body = context.createIrBuilder(result.symbol).irBlockBody(result) {
            lateinit var thisVar: IrValueDeclaration

            fun IrBuilderWithScope.genReturnValue(): IrExpression = if (irConstructor.isPrimary) {
                irCall(symbols.theUnitInstance)
            } else {
                irGet(thisVar)
            }

            val parameterMapping = result.valueParameters.associateBy {
                irConstructor.valueParameters[it.index].symbol
            }

            (irConstructor.body as IrBlockBody).statements.forEach { statement ->
                statement.setDeclarationsParent(result)
                +statement.transformStatement(object : IrElementTransformerVoid() {
                    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                        expression.transformChildrenVoid()

                        return irBlock(expression) {
                            thisVar = if (irConstructor.isPrimary) {
                                // Note: block is empty in this case.
                                result.valueParameters.single()
                            } else {
                                val value = lowerConstructorCallToValue(expression, expression.symbol.owner)
                                irTemporary(value)
                            }
                        }
                    }

                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        expression.transformChildrenVoid()
                        if (expression.symbol == irClass.thisReceiver?.symbol) {
                            return irGet(thisVar)
                        }

                        parameterMapping[expression.symbol]?.let { return irGet(it) }
                        return expression
                    }

                    override fun visitSetValue(expression: IrSetValue): IrExpression {
                        expression.transformChildrenVoid()
                        parameterMapping[expression.symbol]?.let { return irSet(it.symbol, expression.value) }
                        return expression
                    }

                    override fun visitReturn(expression: IrReturn): IrExpression {
                        expression.transformChildrenVoid()
                        if (expression.returnTargetSymbol == irConstructor.symbol) {
                            return irReturn(irBlock(expression.startOffset, expression.endOffset) {
                                +expression.value
                                +genReturnValue()
                            })
                        }

                        return expression
                    }
                })
            }
            +irReturn(genReturnValue())
        }
    }

    private fun getInlineClassBackingField(irClass: IrClass): IrField =
            irClass.declarations.filterIsInstance<IrProperty>().mapNotNull { it.backingField?.takeUnless { it.isStatic } }.single()
}

private var IrConstructor.loweredInlineClassConstructor: IrSimpleFunction? by irAttribute(followAttributeOwner = false)

private fun Context.getLoweredInlineClassConstructor(irConstructor: IrConstructor): IrSimpleFunction = irConstructor::loweredInlineClassConstructor.getOrSetIfNull {
    require(irConstructor.constructedClass.isInlined())

    val returnType = if (irConstructor.isPrimary) {
        // Optimization. When constructor is primary, the return value will be the same as the argument.
        // So we can just use the argument on the call site.
        // This might be especially important for reference types,
        // to avoid redundant suboptimal "slot" machinery messing with this code.
        irBuiltIns.unitType
    } else {
        irConstructor.returnType
    }

    irFactory.buildFun {
        startOffset = irConstructor.startOffset
        endOffset = irConstructor.endOffset
        name = Name.special("<constructor>")
        visibility = irConstructor.visibility
        this.returnType = returnType
    }.apply {
        parent = irConstructor.parent

        // Note: technically speaking, this function doesn't have access to class type parameters (since it is "static").
        // But, technically speaking, otherwise we would have to remap types in the entire IR subtree,
        // which is an overkill here, because type parameters don't matter at this phase of compilation and later.
        // So it is just a trick to make [copyTo] happy:
        val remapTypeMap = irConstructor.constructedClass.typeParameters.associateBy { it }

        valueParameters = irConstructor.valueParameters.map { it.copyTo(this, remapTypeMap = remapTypeMap) }
    }
}
