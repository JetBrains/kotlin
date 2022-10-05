/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dispatcher.ir.generators

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.common.IrDefaultElementVisitor
import org.jetbrains.kotlin.dispatcher.common.FqnUtils
import org.jetbrains.kotlin.dispatcher.fir.AddAbstractGetKindFunctionExtension
import org.jetbrains.kotlin.dispatcher.fir.AddDispatchFunctionExtension
import org.jetbrains.kotlin.dispatcher.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSubclassOf

class DispatchFunctionBodyGenerator(
    private val dispatcherContext: DispatcherPluginContext,
    private val context: IrPluginContext): IrDefaultElementVisitor() {
    private val builtIns = context.symbols.irBuiltIns

    override fun visitClass(declaration: IrClass) {
        if (!declaration.hasAnnotation(FqnUtils.DispatchedVisitor.GENERATE_DISPATCH_FUNCTION_FQN)) {
            return
        }

        val dispatchedVisitor = declaration.getDispatchedVisitor()
        require(dispatchedVisitor != null) { "Class ${declaration.name} hasn't @DispatchedVisitor parent" }

        val info = dispatcherContext.dispatchedVisitorInfoProvider.getInfo(dispatchedVisitor)

        val dispatchFunctions = declaration.functions.filter {
            val origin = it.origin
            origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey == AddDispatchFunctionExtension.Key
        }

        val allVisitFunctions = getDispatchedFunctionProjection(declaration).map {
            val enumEntry = it.valueParameters[0].type.getClass()?.getWithKindEnumEntry()
            require(enumEntry != null) { "Parameter of ${declaration.name}:${it.name} function is not correct" }
            Pair(it, enumEntry)
        }

        for (dispatchFunction in dispatchFunctions) {
            dispatchFunction.body = generateBody(dispatchFunction, allVisitFunctions, info)
        }
    }

    private fun generateBody(dispatchFunction: IrSimpleFunction, allVisitFunctions: List<Pair<IrSimpleFunction, IrEnumEntry>>,
                             info: DispatchedVisitorInfo): IrBlockBody {
        val exprParam = dispatchFunction.valueParameters[0]
        val exprClass = exprParam.type.getClass()
        require(exprClass != null)

        val getKindFunction = exprClass.functions.find {
            it.isGeneratedBy(AddAbstractGetKindFunctionExtension.Key)
        }
        require(getKindFunction != null)

        val builder = DeclarationIrBuilder(IrGeneratorContextBase(context.irBuiltIns), dispatchFunction.symbol, -1, -1)
        return builder.irBlockBody {
            val exprKindVar = irTemporary(
                value = irCall(getKindFunction).apply { dispatchReceiver = irGet(exprParam) }
            )
            val branches = buildList<IrBranch> {
                for ((visitFun, enumEntry) in allVisitFunctions) {
                    val visitedClass = visitFun.valueParameters[0].type.getClass()
                    require(visitedClass != null)
                    if (!visitedClass.isSubclassOf(exprClass)) {
                        continue
                    }

                    add(irBranch(
                        irEquals(
                            irGet(exprKindVar),
                            IrGetEnumValueImpl(-1, -1, info.enumType, enumEntry.symbol)
                        ),
                        irBlock {
                            +irCall(visitFun).apply {
                                dispatchReceiver = irGet(dispatchFunction.dispatchReceiverParameter!!)
                                putValueArgument(0, irAs(irGet(exprParam), visitFun.valueParameters[0].type))
                            }
                        }
                    ))
                }
            }

            +irWhen(builtIns.unitType, branches)
        }
    }

    private fun IrClass.getWithKindEnumEntry(): IrEnumEntry? {
        val withKindAnn = getAnnotation(FqnUtils.Kind.WITH_KIND_ANNOTATION_FQN)
        require(withKindAnn != null) { "Class $name is not annotated by @${FqnUtils.Kind.WITH_KIND_ANNOTATION_FQN}" }
        val enumType = getConstructorTypeArgument(withKindAnn, 0)!!
        val enumEntryStr = withKindAnn.getConstValueArgument(0, IrConstKind.String)
        return dispatcherContext.enumEntryProvider.getEnumEntryByName(enumType, enumEntryStr)
    }

    private fun getDispatchedFunctionProjection(irClass: IrClass): List<IrSimpleFunction> {
        val overriddenFunctions = irClass.functions.filter { !it.isFakeOverride && it.overridesDispatchFunction() }.toMutableList()
        val processed = overriddenFunctions.map { it.name }.toMutableSet()

        for (parent in irClass.getSuperTypesClassSequence()) {
            if (parent.hasAnnotation(FqnUtils.DispatchedVisitor.DISPATCHED_VISITOR_ANNOTATION_FQN)) {
                break
            }
            val newOverriddenFunctions = parent.functions.filter {
                !it.isFakeOverride && it.overridesDispatchFunction() && it.name !in processed
            }.toList()
            processed += newOverriddenFunctions.map { it.name }
            overriddenFunctions.addAll(newOverriddenFunctions)
        }
        return overriddenFunctions
    }

    private fun IrSimpleFunction.overridesDispatchFunction(): Boolean {
        return getMainOverriddenSequence().any {
            it.hasAnnotation(FqnUtils.DispatchedVisitor.DISPATCHED_ANNOTATION_FQN)
        }
    }

    private fun IrClass.getDispatchedVisitor(): IrClass? {
        return getSuperTypesClassSequence().find {
            it.hasAnnotation(FqnUtils.DispatchedVisitor.DISPATCHED_VISITOR_ANNOTATION_FQN)
        }
    }
}