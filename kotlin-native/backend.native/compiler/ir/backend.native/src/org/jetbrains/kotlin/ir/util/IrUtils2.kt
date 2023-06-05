/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.checkers.isRestrictsSuspensionReceiver
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import java.lang.reflect.Proxy

internal fun irBuilder(
        irBuiltIns: IrBuiltIns,
        scopeOwnerSymbol: IrSymbol,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
): IrBuilderWithScope =
        object : IrBuilderWithScope(
                IrGeneratorContextBase(irBuiltIns),
                Scope(scopeOwnerSymbol),
                startOffset,
                endOffset
        ) {}

private var topLevelInitializersCounter = 0

internal fun IrFile.addTopLevelInitializer(expression: IrExpression, context: KonanBackendContext, threadLocal: Boolean, eager: Boolean) {
    val irField = IrFieldImpl(
            expression.startOffset, expression.endOffset,
            IrDeclarationOrigin.DEFINED,
            IrFieldSymbolImpl(),
            "topLevelInitializer${topLevelInitializersCounter++}".synthesizedName,
            expression.type,
            DescriptorVisibilities.PRIVATE,
            isFinal = true,
            isExternal = false,
            isStatic = true,
    ).apply {
        expression.setDeclarationsParent(this)

        if (threadLocal)
            annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, context.ir.symbols.threadLocal.owner)

        if (eager)
            annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, context.ir.symbols.eagerInitialization.owner)

        initializer = IrExpressionBodyImpl(startOffset, endOffset, expression)
    }
    addChild(irField)
}

fun IrModuleFragment.addFile(fileEntry: IrFileEntry, packageFqName: FqName): IrFile {
    val packageFragmentDescriptor = object : PackageFragmentDescriptorImpl(this.descriptor, packageFqName) {
        override fun getMemberScope(): MemberScope = MemberScope.Empty
    }

    return IrFileImpl(fileEntry, packageFragmentDescriptor)
            .also { this.files += it }
}

inline fun <reified T> stub(name: String): T {
    return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) {
        _ /* proxy */, method, _ /* methodArgs */ ->
        if (method.name == "toString" && method.parameterCount == 0) {
            "${T::class.simpleName} stub for $name"
        } else {
            error("${T::class.simpleName}.${method.name} is not supported for $name")
        }
    } as T
}

fun IrDeclarationContainer.addChildren(declarations: List<IrDeclaration>) {
    declarations.forEach { this.addChild(it) }
}

fun IrDeclarationContainer.addChild(declaration: IrDeclaration) {
    this.declarations += declaration
    declaration.accept(SetDeclarationsParentVisitor, this)
}

tailrec fun IrDeclaration.getContainingFile(): IrFile? {
    val parent = this.parent

    return when (parent) {
        is IrFile -> parent
        is IrDeclaration -> parent.getContainingFile()
        else -> null
    }
}

fun IrFunctionAccessExpression.addArguments(args: Map<IrValueParameter, IrExpression>) {
    val unhandledParameters = args.keys.toMutableSet()
    fun getArg(parameter: IrValueParameter) = args[parameter]?.also { unhandledParameters -= parameter }

    symbol.owner.dispatchReceiverParameter?.let {
        val arg = getArg(it)
        if (arg != null) {
            this.dispatchReceiver = arg
        }
    }

    symbol.owner.extensionReceiverParameter?.let {
        val arg = getArg(it)
        if (arg != null) {
            this.extensionReceiver = arg
        }
    }

    symbol.owner.valueParameters.forEach {
        val arg = getArg(it)
        if (arg != null) {
            this.putValueArgument(it.index, arg)
        }
    }
}

fun IrType.substitute(map: Map<IrTypeParameterSymbol, IrType>): IrType {
    if (this !is IrSimpleType) return this

    return when (val classifier = this.classifier) {
        is IrTypeParameterSymbol ->
            map[classifier]?.mergeNullability(this) ?: this
        is IrClassSymbol -> if (this.arguments.isEmpty()) {
            this // Fast path.
        } else {
            val newArguments = this.arguments.map {
                when (it) {
                    is IrTypeProjection -> makeTypeProjection(it.type.substitute(map), it.variance)
                    is IrStarProjection -> it
                }
            }
            IrSimpleTypeImpl(classifier, nullability, newArguments, annotations)
        }
        is IrScriptSymbol -> classifier.unexpectedSymbolKind<IrClassifierSymbol>()
    }

}

private fun IrFunction.substitutedReturnType(typeArguments: List<IrType>): IrType {
    val unsubstituted = this.returnType
    if (typeArguments.isEmpty()) return unsubstituted // Fast path.
    if (this is IrConstructor) {
        // Workaround for missing type parameters in constructors. TODO: remove.
        return this.returnType.classifierOrFail.typeWith(typeArguments)
    }

    assert(this.typeParameters.size >= typeArguments.size) // TODO: check equality.
    // TODO: receiver type must also be considered.
    return unsubstituted.substitute(this.typeParameters.map { it.symbol }.zip(typeArguments).toMap())
}

// TODO: this function must be avoided since it takes symbol's owner implicitly.
fun IrBuilderWithScope.irCall(symbol: IrFunctionSymbol, typeArguments: List<IrType> = emptyList()) =
        this.irCall(symbol, symbol.owner.substitutedReturnType(typeArguments), typeArguments)

fun IrBuilderWithScope.irCall(irFunction: IrFunction, typeArguments: List<IrType> = emptyList()) =
        irCall(irFunction.symbol, typeArguments)

internal fun irCall(startOffset: Int, endOffset: Int, irFunction: IrSimpleFunction, typeArguments: List<IrType>): IrCall =
        IrCallImpl.fromSymbolOwner(
                startOffset, endOffset, irFunction.substitutedReturnType(typeArguments),
                irFunction.symbol, typeArguments.size, irFunction.valueParameters.size
        ).apply {
            typeArguments.forEachIndexed { index, irType ->
                this.putTypeArgument(index, irType)
            }
        }

fun IrBuilderWithScope.irCallOp(
        callee: IrFunction,
        dispatchReceiver: IrExpression,
        argument: IrExpression
) =
        irCall(callee).apply {
            this.dispatchReceiver = dispatchReceiver
            putValueArgument(0, argument)
        }

fun IrBuilderWithScope.irSetVar(variable: IrVariable, value: IrExpression) =
        irSet(variable.symbol, value)

fun IrBuilderWithScope.irCatch(type: IrType) =
        IrCatchImpl(
                startOffset, endOffset,
                IrVariableImpl(
                        startOffset,
                        endOffset,
                        IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                        IrVariableSymbolImpl(),
                        Name.identifier("e"),
                        type,
                        false,
                        false,
                        false
                ).apply {
                    parent = this@irCatch.parent
                }
        )

/**
 * Binds the arguments explicitly represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
fun IrMemberAccessExpression<*>.getArgumentsWithIr(): List<Pair<IrValueParameter, IrExpression>> {
    val res = mutableListOf<Pair<IrValueParameter, IrExpression>>()
    val irFunction = when (this) {
        is IrFunctionAccessExpression -> this.symbol.owner
        is IrFunctionReference -> this.symbol.owner
        else -> error(this)
    }

    dispatchReceiver?.let {
        res += (irFunction.dispatchReceiverParameter!! to it)
    }

    extensionReceiver?.let {
        res += (irFunction.extensionReceiverParameter!! to it)
    }

    irFunction.valueParameters.forEachIndexed { index, it ->
        val arg = getValueArgument(index)
        if (arg != null) {
            res += (it to arg)
        }
    }

    return res
}

fun CommonBackendContext.createArrayOfExpression(
        startOffset: Int, endOffset: Int,
        arrayElementType: IrType,
        arrayElements: List<IrExpression>
): IrExpression {

    val arrayType = ir.symbols.array.typeWith(arrayElementType)
    val arg0 = IrVarargImpl(startOffset, endOffset, arrayType, arrayElementType, arrayElements)

    return irCall(startOffset, endOffset, ir.symbols.arrayOf.owner, listOf(arrayElementType)).apply {
        putValueArgument(0, arg0)
    }
}

fun createField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        type: IrType,
        name: Name,
        isMutable: Boolean,
        owner: IrClass
) =
    IrFieldImpl(
            startOffset, endOffset,
            origin,
            IrFieldSymbolImpl(),
            name,
            type,
            DescriptorVisibilities.PRIVATE,
            !isMutable,
            false,
            false,
    ).apply {
        owner.declarations += this
        parent = owner
    }

fun IrValueParameter.copy(newDescriptor: ParameterDescriptor): IrValueParameter {
    // Aggressive use of IrBasedDescriptors during deserialization
    // makes these types different.
    // Let's hope they not really used afterwards.
    //assert(this.descriptor.type == newDescriptor.type) {
     //   "type1 = ${this.descriptor.type} != type2 = ${newDescriptor.type}"
    //}

    return IrValueParameterImpl(
        startOffset, endOffset, IrDeclarationOrigin.DEFINED, IrValueParameterSymbolImpl(newDescriptor),
        newDescriptor.name, newDescriptor.indexOrMinusOne, type, varargElementType,
        newDescriptor.isCrossinline, newDescriptor.isNoinline,
        isHidden = false, isAssignable = false
    )
}

fun IrClass.defaultOrNullableType(hasQuestionMark: Boolean) =
        if (hasQuestionMark) this.defaultType.makeNullable() else this.defaultType

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrFunction.isRestrictedSuspendFunction(): Boolean =
        this.descriptor.extensionReceiverParameter?.type?.isRestrictsSuspensionReceiver() == true

fun IrBuilderWithScope.irByte(value: Byte) =
        IrConstImpl.byte(startOffset, endOffset, context.irBuiltIns.byteType, value)

val IrField.hasNonConstInitializer: Boolean
    get() = initializer?.expression.let { it != null && it !is IrConst<*> && it !is IrConstantValue }
