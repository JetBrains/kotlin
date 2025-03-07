/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrErrorCallExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.columns.ColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

class IrBodyFiller : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        DataFrameFileLowering(pluginContext).lower(moduleFragment)
    }
}

private class DataFrameFileLowering(val context: IrPluginContext) : FileLoweringPass, IrElementTransformerVoid() {
    companion object {
        val COLUMNS_CONTAINER_ID =
            CallableId(ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("ColumnsContainer")), Name.identifier("get"))
        val COLUMNS_SCOPE_ID =
            CallableId(ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("ColumnsScope")), Name.identifier("get"))
        val DATA_ROW_ID =
            CallableId(ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("DataRow")), Name.identifier("get"))
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, null)
    }

    override fun visitConstructor(declaration: IrConstructor): IrStatement {
        val origin = declaration.origin
        if (!(origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey is TokenGenerator.Key)) return declaration
        declaration.body = generateBodyForDefaultConstructor(declaration)
        return declaration
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun generateBodyForDefaultConstructor(declaration: IrConstructor): IrBody? {
        val irType = declaration.returnType.superTypes()[0]
        val symbol = irType.classOrFail.owner.primaryConstructor?.symbol ?: return null
        val type = declaration.returnType as? IrSimpleType ?: return null
        val delegatingAnyCall = IrDelegatingConstructorCallImpl(
            -1,
            -1,
            irType,
            symbol,
            typeArgumentsCount = 0,
        ).also { it.copyAttributes(declaration.parentAsClass) }

        val initializerCall = IrInstanceInitializerCallImpl(
            -1,
            -1,
            (declaration.parent as? IrClass)?.symbol ?: return null,
            type
        )

        return context.irFactory.createBlockBody(-1, -1, listOf(delegatingAnyCall, initializerCall))
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitProperty(declaration: IrProperty): IrStatement {
        val origin = declaration.origin
        val pluginKey = (origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey as? DataFramePlugin
        if (pluginKey == null) {
            declaration.transformChildren(this, null)
            return declaration
        }
        val getter = declaration.getter ?: return declaration

        val constructors = context.referenceConstructors(ClassId(FqName("kotlin.jvm"), Name.identifier("JvmName")))
        val jvmName = constructors.single { it.owner.valueParameters.size == 1 }
        val marker =
            ((getter.extensionReceiverParameter!!.type as IrSimpleType).arguments.single() as IrSimpleType).classOrFail.owner
        val jvmNameArg = "${marker.nestedName()}_${declaration.name.identifier}"
        getter.annotations = listOf(
            IrConstructorCallImpl(-1, -1, jvmName.owner.returnType, jvmName, 0, 1)
                .also {
                    it.putValueArgument(0, IrConstImpl.string(-1, -1, context.irBuiltIns.stringType, jvmNameArg))
                }
        )
        val returnType = getter.returnType
        val isDataColumn = returnType.classFqName!!.asString().let {
            it == DataColumn::class.qualifiedName!! || it == ColumnGroup::class.qualifiedName!!
        }

        val get = if (isDataColumn) {
            context
                .referenceFunctions(COLUMNS_SCOPE_ID)
                .single {
                    it.owner.valueParameters.size == 1 && it.owner.valueParameters[0].type == context.irBuiltIns.stringType
                }
        } else {
            context
                .referenceFunctions(DATA_ROW_ID)
                .single {
                    it.owner.valueParameters.size == 1 && it.owner.valueParameters[0].type == context.irBuiltIns.stringType
                }
        }

        val call = IrCallImpl(-1, -1, context.irBuiltIns.anyNType, get, 0).also {
            val thisSymbol: IrValueSymbol = getter.extensionReceiverParameter?.symbol!!
            it.dispatchReceiver = IrGetValueImpl(-1, -1, thisSymbol)
            val annotation = declaration.annotations.findAnnotation(Names.COLUMN_NAME_ANNOTATION.asSingleFqName())
            val columnName = (annotation?.valueArguments?.get(0) as? IrConst)?.value as? String
            val columName = columnName ?: declaration.name.identifier
            it.putValueArgument(0, IrConstImpl.string(-1, -1, context.irBuiltIns.stringType, columName))
        }

        val typeOp = IrTypeOperatorCallImpl(-1, -1, returnType, IrTypeOperator.CAST, returnType, call)
        val returnExpression = IrReturnImpl(-1, -1, returnType, getter.symbol, typeOp)
        getter.apply {
            body = factory.createBlockBody(-1, -1, listOf(returnExpression))
        }

        return declaration
    }

    private fun IrDeclarationWithName.nestedName() = buildString { computeNestedName(this@nestedName, this) }

    private fun computeNestedName(declaration: IrDeclarationWithName, result: StringBuilder): Boolean {
        when (val parent = declaration.parent) {
            is IrClass -> {
                if (!computeNestedName(parent, result)) return false
            }
            is IrPackageFragment -> {}
            else -> return false
        }
        if (result.isNotEmpty()) result.append('_')
        result.append(declaration.name.asString())
        return true
    }

    // org.jetbrains.kotlin.fir.backend.generators.CallAndReferenceGenerator#applyReceivers
    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        if (isScope(expression.typeOperand)) {
            return expression.replaceWithConstructorCall()
        }
        return super.visitTypeOperator(expression)
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression): IrExpression {
        if (!isScope(expression.type)) {
            return expression
        }
        return expression.replaceWithConstructorCall()
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun isScope(type: IrType): Boolean {
        val origin = (type.classifierOrNull?.owner as? IrClass)?.origin ?: return false
        val fromPlugin = origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey is DataFramePlugin
        val scopeReference = type.classFqName?.shortName()?.asString()?.startsWith("Scope") ?: false
        return fromPlugin || scopeReference
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrExpression.replaceWithConstructorCall(): IrConstructorCallImpl {
        val constructor = type.getClass()!!.constructors.toList().single()
        return IrConstructorCallImpl(-1, -1, type, constructor.symbol, 0, 0)
    }
}
