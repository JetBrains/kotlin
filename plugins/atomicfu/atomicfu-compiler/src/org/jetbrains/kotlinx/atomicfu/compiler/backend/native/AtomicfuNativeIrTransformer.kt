/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm.JvmAtomicSymbols
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.capture
import org.jetbrains.kotlinx.atomicfu.compiler.backend.getValueArguments

private const val AFU_PKG = "kotlinx.atomicfu"
private const val ATOMIC_VALUE_FACTORY = "atomic"

class AtomicfuNativeIrTransformer(
    val context: IrPluginContext,
    val atomicSymbols: NativeAtomicSymbols
) {
    private val ATOMIC_VALUE_TYPES = setOf("AtomicInt", "AtomicLong", "AtomicBoolean", "AtomicRef")

    private val AFU_VALUE_TYPES: Map<String, IrType> = mapOf(
        "AtomicInt" to context.irBuiltIns.intType,
        "AtomicLong" to context.irBuiltIns.longType,
        "AtomicBoolean" to context.irBuiltIns.booleanType,
        "AtomicRef" to context.irBuiltIns.anyNType
    )

    fun transform(moduleFragment: IrModuleFragment) {
        transformAtomicFields(moduleFragment)
        transformAtomicCalls(moduleFragment)
    }

    private fun transformAtomicFields(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(AtomicHandlerTransformer(), null)
        }
    }

    private fun transformAtomicCalls(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(AtomicfuTransformer(), null)
        }
    }

    private inner class AtomicHandlerTransformer : IrElementTransformer<IrFunction?> {
        override fun visitClass(declaration: IrClass, data: IrFunction?): IrStatement {
            declaration.declarations.filter(::fromKotlinxAtomicfu).forEach {
                (it as IrProperty).transformAtomicfuProperty(declaration)
            }
            return super.visitClass(declaration, data)
        }

        private fun IrProperty.transformAtomicfuProperty(parent: IrDeclarationContainer) {
            when {
                isAtomic() -> {
                    println(parent)
                    transformAtomicProperty()
                }
                else -> error("Property type not supported")
            }
        }

        private fun IrProperty.transformAtomicProperty() {
            backingField?.let { backingField ->
                backingField.initializer?.let {
                    val initializer = it.expression as IrCall
                    when {
                        initializer.isAtomicFactory() -> {
                            // val a = atomic(77) -> val a = kotlin.native.concurrent.AtomicInt(77)
                            with(atomicSymbols.createBuilder(this.symbol)) {
                                it.expression = irCall(atomicSymbols.atomicIntNativeConstructor)
                            }
                        }
                        else -> error("Unexpected initializer of the delegated property: $initializer")
                    }
                }
            }
        }
    }

    private inner class AtomicfuTransformer : IrElementTransformer<IrFunction?> {
        override fun visitCall(expression: IrCall, data: IrFunction?): IrElement {
            (expression.extensionReceiver ?: expression.dispatchReceiver)?.transform(this, data)?.let {
                with(atomicSymbols.createBuilder(expression.symbol)) {
                    val receiver = if (it is IrTypeOperatorCallImpl) it.argument else it
                    if (receiver.type.isAtomicValueType()) { // todo only AtomicInt is supported
                        val functionName = expression.symbol.owner.name.asString()
                        val irCall = irCall(atomicSymbols.atomicIntNativeClass.getSimpleFunction(functionName)!!).apply {
                            this.dispatchReceiver = (receiver as IrCall).dispatchReceiver
                            expression.getValueArguments().forEachIndexed { index, arg ->
                                putValueArgument(index, arg)
                            }
                        }
                        return super.visitCall(irCall, data)
                    }
                }
            }
            return super.visitCall(expression, data)
        }
    }

    private fun IrProperty.isAtomic(): Boolean =
        !isDelegated && backingField?.type?.isAtomicValueType() ?: false

    private fun IrType.isAtomicValueType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() in ATOMIC_VALUE_TYPES
        } ?: false

    private fun IrType.atomicToValueType(): IrType =
        classFqName?.let {
            AFU_VALUE_TYPES[it.shortName().asString()]
        } ?: error("No corresponding value type was found for this atomic type: ${this.render()}")

    private fun fromKotlinxAtomicfu(declaration: IrDeclaration): Boolean =
        declaration is IrProperty &&
                declaration.backingField?.type?.isKotlinxAtomicfuPackage() ?: false

    private fun IrType.isKotlinxAtomicfuPackage() =
        classFqName?.let { it.parent().asString() == AFU_PKG } ?: false

    private fun IrCall.isAtomicFactory(): Boolean =
        symbol.isKotlinxAtomicfuPackage() && symbol.owner.name.asString() == ATOMIC_VALUE_FACTORY &&
                type.isAtomicValueType()

    // todo abstract
    private fun IrSimpleFunctionSymbol.isKotlinxAtomicfuPackage(): Boolean =
        owner.parent.kotlinFqName.asString() == AFU_PKG
}
