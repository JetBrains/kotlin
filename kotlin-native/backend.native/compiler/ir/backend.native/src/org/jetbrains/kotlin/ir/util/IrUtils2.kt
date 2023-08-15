/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCatchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.checkers.isRestrictsSuspensionReceiver
import org.jetbrains.kotlin.resolve.scopes.MemberScope

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

fun IrClass.defaultOrNullableType(hasQuestionMark: Boolean) =
        if (hasQuestionMark) this.defaultType.makeNullable() else this.defaultType

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrFunction.isRestrictedSuspendFunction(): Boolean =
        this.descriptor.extensionReceiverParameter?.type?.isRestrictsSuspensionReceiver() == true

fun IrBuilderWithScope.irByte(value: Byte) =
        IrConstImpl.byte(startOffset, endOffset, context.irBuiltIns.byteType, value)

val IrField.hasNonConstInitializer: Boolean
    get() = initializer?.expression.let { it != null && it !is IrConst<*> && it !is IrConstantValue }
