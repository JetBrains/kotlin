/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm.AtomicSymbols
import org.jetbrains.kotlin.ir.builders.*

private const val AFU_PKG = "kotlinx.atomicfu"
private const val TRACE_BASE_TYPE = "TraceBase"
private const val ATOMIC_VALUE_FACTORY = "atomic"
private const val INVOKE = "invoke"
private const val APPEND = "append"
private const val GET = "get"
private const val ATOMICFU = "atomicfu"
private const val ATOMIC_ARRAY_RECEIVER_SUFFIX = "\$array"
private const val DISPATCH_RECEIVER = "${ATOMICFU}\$dispatchReceiver"
private const val ATOMIC_HANDLER = "${ATOMICFU}\$handler"
private const val ACTION = "${ATOMICFU}\$action"
private const val INDEX = "${ATOMICFU}\$index"
private const val VOLATILE_WRAPPER_SUFFIX = "\$VolatileWrapper"
private const val LOOP = "loop"
private const val UPDATE = "update"

class AtomicfuNativeIrTransformer(
    val context: IrPluginContext,
    val atomicSymbols: AtomicSymbols
) {
    private val ATOMIC_VALUE_TYPES = setOf("AtomicInt", "AtomicLong", "AtomicBoolean", "AtomicRef")

    fun transform(moduleFragment: IrModuleFragment) {
        transformAtomicFields(moduleFragment)
    }

    private fun transformAtomicFields(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(AtomicHandlerTransformer(), null)
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
                    transformAtomicProperty(parent as IrClass)
                }
                else -> error("Property type not supported")
            }
        }

        private fun IrProperty.transformAtomicProperty(parentClass: IrClass) {
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

        private fun IrProperty.isAtomic(): Boolean =
            !isDelegated && backingField?.type?.isAtomicValueType() ?: false

        private fun IrType.isAtomicValueType() =
            classFqName?.let {
                it.parent().asString() == AFU_PKG && it.shortName().asString() in ATOMIC_VALUE_TYPES
            } ?: false

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
}
