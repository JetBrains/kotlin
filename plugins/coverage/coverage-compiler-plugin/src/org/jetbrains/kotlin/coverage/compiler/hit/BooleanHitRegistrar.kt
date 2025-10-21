/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.coverage.compiler.hit

import org.jetbrains.kotlin.coverage.compiler.common.KotlinCoverageInstrumentationContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.name.Name

internal class BooleanHitRegistrar(val moduleId: Int, val segmentNumber: Int, val context: KotlinCoverageInstrumentationContext) :
    HitRegistrar {
    private val segmentPropertyName = Name.identifier($$"$kover_segment_")
    private val segmentGetterName = Name.identifier($$"$kover_get_segment_")

    private val segmentProperty: IrProperty = context.irFactory.createProperty(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.declarationOrigin,
        segmentPropertyName,
        DescriptorVisibilities.PRIVATE,
        Modality.FINAL,
        IrPropertySymbolImpl(),
        isVar = true,
        isConst = false,
        isLateinit = false,
        isDelegated = false,
    )

    private val segmentGetter: IrSimpleFunction = context.irFactory.createSimpleFunction(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.declarationOrigin,
        segmentGetterName,
        DescriptorVisibilities.PRIVATE,
        isInline = false,
        isExpect = false,
        context.builtIns.primitiveArrayOfBooleanType,
        Modality.FINAL,
        IrSimpleFunctionSymbolImpl(),
        isTailrec = false,
        isSuspend = false,
        isOperator = false,
        isInfix = false
    ).also { function -> function.returnType = context.builtIns.primitiveArrayOfBooleanType}

    val pointsCounter = Counter()

    override val extraDeclarations: List<IrDeclaration> = listOf(segmentProperty, segmentGetter)

    override fun body(irFunction: IrFunction): BlockWithExecutionPoints {
        return BooleanBlockWithExecutionPoints(segmentGetter, irFunction, pointsCounter, context)
    }

    override fun finalize() {
        val myCall = context.factory.call(context.runtime.getOrCreateBooleanSegmentFun, context.builtIns.primitiveArrayOfBooleanType) {
            arguments[0] = context.factory.getObjectValue(context.runtime.booleanStorageClass)
            arguments[1] = context.factory.intConst(moduleId)
            arguments[2] = context.factory.intConst(segmentNumber)
            arguments[3] = context.factory.intConst(pointsCounter.count)
        }

        val returnCall = context.factory.returnValue(segmentGetter.symbol, myCall)

        segmentGetter.body = context.factory.block {
            add(returnCall)
        }
    }
}

private class BooleanBlockWithExecutionPoints(
    private val segmentGetter: IrSimpleFunction,
    private val thisFunction: IrFunction,
    private val counter: Counter,
    val context: KotlinCoverageInstrumentationContext,
) : BlockWithExecutionPoints {
    override val pointsCount: Int
        get() = counter.count

    private val variableName = Name.identifier($$"$coverage_segment")

    private val variable: IrVariable = context.factory.`val`(
        variableName,
        context.builtIns.primitiveArrayOfBooleanType,
        thisFunction,
        context.factory.call(segmentGetter.symbol, context.builtIns.primitiveArrayOfBooleanType)
    )

    override val firstStatement: IrStatement = variable

    override fun registerPoint(): ExecutionPoint {
        val id = counter.count++

        val statement = context.factory.call(context.builtIns.primitiveArrayOfBooleansSetter, context.builtIns.unitType) {
            arguments[0] = context.factory.getValue(variable)
            arguments[1] = context.factory.intConst(id)
            arguments[2] = context.factory.booleanConst(true)
        }

        return ExecutionPoint(id, statement)
    }
}

internal class Counter(var count: Int = 0)