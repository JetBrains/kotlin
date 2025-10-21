/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalSymbolFinderAPI::class, UnsafeDuringIrConstructionAPI::class)

package org.jetbrains.kotlin.coverage.compiler.common

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.coverage.compiler.common.Constants.BOOLEAN_STORAGE_NAME
import org.jetbrains.kotlin.coverage.compiler.common.Constants.CREATE_BOOLEAN_SEGMENT_NAME
import org.jetbrains.kotlin.coverage.compiler.common.Constants.KOTLIN_COVERAGE_DECLARATION_ORIGIN
import org.jetbrains.kotlin.coverage.compiler.common.Constants.KOTLIN_COVERAGE_STATEMENT_ORIGIN
import org.jetbrains.kotlin.coverage.compiler.common.Constants.LET_NAME
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KotlinCoverageInstrumentationContext(pluginContext: IrPluginContext) {
    val irFactory = pluginContext.irFactory

    val declarationOrigin = KOTLIN_COVERAGE_DECLARATION_ORIGIN
    val statementOrigin = KOTLIN_COVERAGE_STATEMENT_ORIGIN

    val builtIns = KotlinBuiltIns(pluginContext)
    val runtime = CoverageRuntime(pluginContext)
    val factory = Factory(builtIns, irFactory)
}

class KotlinBuiltIns(private val pluginContext: IrPluginContext) {
    val unitType = pluginContext.irBuiltIns.unitType
    val unitClass = pluginContext.irBuiltIns.unitClass
    val nothingType = pluginContext.irBuiltIns.nothingType
    val stringType = pluginContext.irBuiltIns.stringType
    val intType = pluginContext.irBuiltIns.intType
    val booleanType = pluginContext.irBuiltIns.booleanType
    val arrayOfPrimitiveBooleansClass = pluginContext.irBuiltIns.primitiveArrayForType.getValue(pluginContext.irBuiltIns.booleanType)
    val primitiveArrayOfBooleanType = arrayOfPrimitiveBooleansClass.defaultType

    val function0 = pluginContext.irBuiltIns.functionN(0)
    val function0InvokeFun = function0.functions.firstOrNull { it.name.asString() == "invoke" }?.symbol
        ?: throw IllegalStateException("Can't find function 'invoke' in the class 'kotlin.Function0'")

    val letFun = pluginContext.referenceFunctions(LET_NAME).firstOrNull()
        ?: throw IllegalStateException("Can't find built-in function '${LET_NAME.asSingleFqName().asString()}'")

    val primitiveArrayOfBooleansSetter = arrayOfPrimitiveBooleansClass.functions.firstOrNull { it.owner.name.asString() == "set" }
        ?: throw IllegalStateException("Can't find function set for primitive byte array")

    fun functionNType(returnType: IrType, types: List<IrType>): IrType {
        val irClass = pluginContext.irBuiltIns.functionN(types.size)

        val typeParams = irClass.typeParameters
        return irClass.defaultType.substitute(typeParams, listOf(returnType) + types)
    }


}

class CoverageRuntime(pluginContext: IrPluginContext) {
    val booleanStorageClass = pluginContext.referenceClass(BOOLEAN_STORAGE_NAME)
        ?: throw IllegalStateException("Can't find class '${BOOLEAN_STORAGE_NAME.asString()}'")
    val getOrCreateBooleanSegmentFun = pluginContext.irBuiltIns.symbolFinder.findFunctions(CREATE_BOOLEAN_SEGMENT_NAME).firstOrNull()
        ?: throw IllegalStateException("Can't find function '${CREATE_BOOLEAN_SEGMENT_NAME.asSingleFqName().asString()}'")
}

class Factory(private val builtIns: KotlinBuiltIns, private val irFactory: IrFactory) {
    fun call(function: IrSimpleFunctionSymbol, returnType: IrType, block: IrCall.() -> Unit = {}): IrCall {
        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            returnType,
            function,
            origin = KOTLIN_COVERAGE_STATEMENT_ORIGIN
        ).also { call ->
            call.block()
        }
    }

    fun `val`(name: Name, type: IrType, parent: IrDeclarationParent, initializer: IrExpression? = null): IrVariable {
        return IrVariableImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            KOTLIN_COVERAGE_DECLARATION_ORIGIN,
            IrVariableSymbolImpl(),
            name,
            type,
            isVar = false,
            isConst = false,
            isLateinit = false,
        ).also { variable ->
            variable.initializer = initializer
            variable.parent = parent
        }
    }

    fun intConst(value: Int): IrConst {
        return IrConstImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            builtIns.intType,
            IrConstKind.Int,
            value
        )
    }

    fun booleanConst(value: Boolean): IrConst {
        return IrConstImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            builtIns.booleanType,
            IrConstKind.Boolean,
            value
        )
    }

    fun getObjectValue(classSymbol: IrClassSymbol): IrGetObjectValue {
        return IrGetObjectValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            classSymbol.defaultType,
            classSymbol
        )
    }

    fun lambda(
        returnType: IrType,
        argumentTypes: List<IrType>,
        parent: IrDeclarationParent,
        builder: MutableList<IrStatement>.() -> Unit,
    ): IrFunctionExpression {
        val function =
            irFactory.createSimpleFunction(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                KOTLIN_COVERAGE_DECLARATION_ORIGIN,
                name = Name.special("<anonymous>"),
                isExternal = false,
                visibility = DescriptorVisibilities.LOCAL,
                containerSource = null,
                isSuspend = false,
                isInline = false,
                isExpect = false,
                modality = Modality.FINAL,
                isFakeOverride = false,
                symbol = IrSimpleFunctionSymbolImpl(),
                isTailrec = false,
                isOperator = false,
                isInfix = false,
                returnType = returnType,
            )
        function.body = block(builder)
        function.parent = parent


        return IrFunctionExpressionImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            builtIns.functionNType(returnType, argumentTypes),
            function,
            KOTLIN_COVERAGE_STATEMENT_ORIGIN
        )
    }

    fun getValue(variable: IrVariable, irType: IrType = variable.type): IrGetValue {
        return IrGetValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            irType,
            variable.symbol,
            KOTLIN_COVERAGE_STATEMENT_ORIGIN
        )
    }

    fun returnValue(returnTarget: IrReturnTargetSymbol, value: IrExpression): IrReturn {
        return IrReturnImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            builtIns.nothingType,
            returnTarget,
            value
        )
    }

    fun block(builder: MutableList<IrStatement>.() -> Unit): IrBlockBody {
        return irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
            statements.builder()
        }
    }
}

private object Constants {
    val KOTLIN_COVERAGE_STATEMENT_ORIGIN = IrStatementOriginImpl("KOTLIN_COVERAGE_ORIGIN")
    val KOTLIN_COVERAGE_DECLARATION_ORIGIN = IrDeclarationOriginImpl("KOTLIN_COVERAGE_ORIGIN", true)

    val PRINTLN_NAME = CallableId(FqName("kotlin.io"), Name.identifier("println"))
    val LET_NAME = CallableId(FqName("kotlin"), Name.identifier("let"))
    val COVERAGE_RUNTIME_PACKAGE = FqName("org.jetbrains.kotlin.coverage.runtime")
    val BOOLEAN_STORAGE_NAME = ClassId(COVERAGE_RUNTIME_PACKAGE, Name.identifier("BooleanHitStorage"))
    val CREATE_BOOLEAN_SEGMENT_NAME = CallableId(BOOLEAN_STORAGE_NAME, Name.identifier("getOrCreateSegment"))
}
