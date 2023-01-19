/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.coroutines.getOrCreateFunctionWithContinuationStub
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * This lowering is a hack, to provide compatibility with leaked jvm implementation detail.
 *
 * On jvm SuspendFunctionN<Args, Ret> is implicitly implementing FunctionN+1<Args, Continuation, Any(?)>,
 * and visa versa, and it can be used for reusing continuation objects for performance improvements.
 *
 * Also, it is used in startCoroutineUninterceptedOrReturn intrinsic.
 *
 * So we are adding corresponding FunctionN+1 into supper types.
 * At the current point, lowered suspend function signature is overriding its invoke method.
 *
 */
internal class AddFunctionSupertypeToSuspendFunctionLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                // Don't need to iterate through children. All local classes are already moved to the top level by this moment.
            }

            override fun visitClass(declaration: IrClass) {
                addMissingSupertypes(declaration)
                declaration.acceptChildrenVoid(this)
            }

            private fun IrSimpleFunction.getLowered() = if (isSuspend)
                getOrCreateFunctionWithContinuationStub(context)
            else
                this

            private fun IrClass.getInvokeFunction() = simpleFunctions().single {
                it.name == OperatorNameConventions.INVOKE
            }.getLowered()

            private fun addOverride(clazz: IrClass, alreadyOverridden: IrType, toOverride: IrType) {
                val alreadyOverriddenFunction = alreadyOverridden.classOrNull!!.owner.getInvokeFunction()
                val functionToOverride = toOverride.classOrNull!!.owner.getInvokeFunction()
                val invokeFunction = clazz.simpleFunctions().single { it.overrides(alreadyOverriddenFunction) }
                if (invokeFunction.modality == Modality.ABSTRACT) return
                clazz.superTypes += toOverride
                invokeFunction.overriddenSymbols += functionToOverride.symbol
            }


            private fun addMissingSupertypes(clazz: IrClass) {
                val suspendFunctionSuperTypes = getAllSubstitutedSupertypes(clazz).filter {
                    // SuspendFunction class is some hack in old Kotlin/Native compiler versions.
                    // It's not used now, but is considered as SuspendFunction-like class in isSuspendFunction util,
                    // if found in old klib. We need just to ignore it.
                    it.isSuspendFunction() && it.classOrNull?.owner?.name?.toString() != "SuspendFunction"
                }.toSet()

                val continuationClassSymbol = context.ir.symbols.continuationClass

                fun IrSimpleType.getClassAt(index: Int) = (this.arguments.getOrNull(index) as? IrTypeProjection)?.type?.classOrNull

                val functionWithContinuationSuperTypes = getAllSubstitutedSupertypes(clazz).filter {
                    it.isFunction() &&
                            it.getClassAt(it.arguments.size - 2) == continuationClassSymbol
                }.toSet()

                for (suspendFunctionType in suspendFunctionSuperTypes) {
                    val functionClassTypeArguments = suspendFunctionType.arguments.mapIndexed { index, argument ->
                        val type = (argument as IrTypeProjection).type
                        if (index == suspendFunctionType.arguments.indices.last) {
                            continuationClassSymbol.typeWith(type)
                        } else {
                            type
                        }
                    } + context.irBuiltIns.anyNType

                    val functionType = context.ir.symbols.functionN(functionClassTypeArguments.size - 1).typeWith(functionClassTypeArguments)

                    addOverride(clazz, suspendFunctionType, functionType)
                }

                for (functionType in functionWithContinuationSuperTypes) {
                    val suspendFunctionClassTypeArguments = functionType.arguments.dropLast(1).mapIndexed { index, argument ->
                        val type = (argument as IrTypeProjection).type
                        if (index == functionType.arguments.indices.last - 1) {
                            require(type.classOrNull == continuationClassSymbol)
                            when (val typeArgument = (type as IrSimpleType).arguments.single()) {
                                is IrTypeProjection -> typeArgument.type
                                is IrStarProjection -> context.irBuiltIns.anyNType
                            }
                        } else {
                            type
                        }
                    }

                    val suspendFunctionType = context.ir.symbols.suspendFunctionN(suspendFunctionClassTypeArguments.size - 1).typeWith(suspendFunctionClassTypeArguments)
                    addOverride(clazz, functionType, suspendFunctionType)
                }
            }

        })
    }

}
