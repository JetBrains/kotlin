/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.columns.ColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.DataFramePlugin
import org.jetbrains.kotlinx.dataframe.plugin.DataFrameTokenContentKey
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

class IrBodyFiller : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        DataFrameFileLowering(pluginContext).lower(moduleFragment)
        IrImportedSchemaGenerator(pluginContext).lower(moduleFragment)
    }
}

private class DataFrameFileLowering(val context: IrPluginContext) : FileLoweringPass,
    IrElementTransformerVoid() {
    companion object {
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
        if (!(origin is IrDeclarationOrigin.GeneratedByPlugin && (origin.pluginKey is DataFrameTokenContentKey || origin.pluginKey is DataFramePlugin))) return declaration
        declaration.body = generateBodyForDefaultConstructor(context, declaration)
        return declaration
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitProperty(declaration: IrProperty): IrStatement {
        val origin = declaration.origin
        val pluginKey = (origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey as? DataFramePlugin
        if (pluginKey == null || declaration.modality == Modality.ABSTRACT) {
            declaration.transformChildren(this, null)
            return declaration
        }
        val getter = declaration.getter ?: return declaration

        val finder = context.finderForBuiltins()
        val constructors = finder.findConstructors(ClassId(FqName("kotlin.jvm"), Name.identifier("JvmName")))
        val jvmName = constructors.single { it.owner.parameters.size == 1 }
        val getterExtensionReceiver = getter.parameters.single { it.kind == IrParameterKind.ExtensionReceiver }
        val marker = ((getterExtensionReceiver.type as IrSimpleType).arguments.single() as IrSimpleType).classOrFail.owner
        val jvmNameArg = "${marker.nestedName()}_${declaration.name.identifier}"
        getter.annotations = listOf(
            IrAnnotationImpl(-1, -1, jvmName.owner.returnType, jvmName, 0, 1)
                .also {
                    it.arguments[0] = IrConstImpl.string(-1, -1, context.irBuiltIns.stringType, jvmNameArg)
                }
        )
        val returnType = getter.returnType
        val typeOp = generateColumnAccessCall(
            receiver = IrGetValueImpl(-1, -1, getterExtensionReceiver.symbol), declaration, returnType, marker
        )
        val returnExpression = IrReturnImpl(-1, -1, returnType, getter.symbol, typeOp)
        getter.apply {
            body = factory.createBlockBody(-1, -1, listOf(returnExpression))
        }

        return declaration
    }

    private fun generateColumnAccessCall(
        receiver: IrExpression,
        property: IrProperty,
        returnType: IrType,
        marker: IrClass?,
    ): IrTypeOperatorCall {
        val finder = context.finderForBuiltins()
        val isDataColumn = returnType.classFqName?.asString()?.let {
            it == DataColumn::class.qualifiedName!! || it == ColumnGroup::class.qualifiedName!!
        } ?: false

        val get = if (isDataColumn) {
            finder
                .findFunctions(COLUMNS_SCOPE_ID)
                .single {
                    it.owner.hasShape(
                        dispatchReceiver = true,
                        regularParameters = 1,
                        parameterTypes = listOf(null, context.irBuiltIns.stringType)
                    )
                }
        } else {
            finder
                .findFunctions(DATA_ROW_ID)
                .single {
                    it.owner.hasShape(
                        dispatchReceiver = true,
                        regularParameters = 1,
                        parameterTypes = listOf(null, context.irBuiltIns.stringType)
                    )
                }
        }

        val call = IrCallImpl(-1, -1, context.irBuiltIns.anyNType, get, 0).also {
            val columnName = marker?.properties?.firstOrNull { it.name == property.name }
                ?.getAnnotationArgumentValue<String>(Names.COLUMN_NAME_ANNOTATION.asSingleFqName(), Names.COLUMN_NAME_ARGUMENT.identifier)
            val arg = columnName ?: property.name.identifier

            it.arguments[0] = receiver
            it.arguments[1] = IrConstImpl.string(-1, -1, context.irBuiltIns.stringType, arg)
        }

        return IrTypeOperatorCallImpl(-1, -1, returnType, IrTypeOperator.CAST, returnType, call)
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

    // Implicit receivers injected by org.jetbrains.kotlinx.dataframe.plugin.extensions.ReturnTypeBasedReceiverInjector
    // don't "exist": they are used for resolve, but there's no value on the stack.
    // We need to find all calls that use them as arguments and generate valid code
    override fun visitCall(expression: IrCall): IrExpression {
        val origin = expression.symbol.owner.origin
        if (expression.origin == IrStatementOrigin.GET_PROPERTY && origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey == DataFramePlugin) {
            val receiverType = expression.symbol.owner.parameters.getOrNull(0)?.type
            if (receiverType != null) {
                if (receiverType.isScope()) {
                    val constructor = receiverType.classOrFail.constructors.single()
                    expression.arguments[0] = IrConstructorCallImpl(-1, -1, receiverType, constructor, 0, 0)
                } else if (expression.arguments.size == 1) {
                    return inlineExtensionProperty(receiverType, extensionPropertyCall = expression)
                }

            }
            return super.visitCall(expression)
        }
        return super.visitCall(expression)
    }

    private fun inlineExtensionProperty(
        receiverType: IrType,
        extensionPropertyCall: IrCall,
    ): IrTypeOperatorCall = generateColumnAccessCall(
        receiver = extensionPropertyCall.arguments.getOrNull(0)?.transform(this, null)
            ?: errorWithAttachment("Generated property with unexpected shape") {
                withEntry("Expression", extensionPropertyCall) { it.render() }
                withEntry("Symbol", extensionPropertyCall.symbol.owner) { it.render() }
            },
        extensionPropertyCall.symbol.owner.correspondingPropertySymbol!!.owner,
        extensionPropertyCall.type,
        marker = ((receiverType as IrSimpleType).arguments[0] as IrSimpleType).classOrFail.owner
    )

    private fun IrType.isScope(): Boolean {
        val origin = (classifierOrNull?.owner as? IrClass)?.origin ?: return false
        val fromPlugin = origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey is DataFramePlugin
        val scopeReference = classFqName?.shortName()?.asString()?.startsWith("Scope") ?: false
        return fromPlugin || scopeReference
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun generateBodyForDefaultConstructor(context: IrPluginContext, declaration: IrConstructor): IrBody? {
    val irType = declaration.returnType.superTypes()[0]
    val symbol =
        irType.classOrFail.owner.primaryConstructor?.symbol ?: context.irBuiltIns.anyType.classOrNull?.constructors?.firstOrNull()
        ?: return null
    val type = declaration.returnType as? IrSimpleType ?: return null
    return context.irBuiltIns.createIrBuilder(declaration.symbol).run {
        irBlockBody {
            +irDelegatingConstructorCall(symbol.owner).also { it.copyAttributes(declaration.parentAsClass) }
            +IrInstanceInitializerCallImpl(
                startOffset = -1,
                endOffset = -1,
                classSymbol = (declaration.parent as? IrClass)?.symbol ?: return null,
                type = type
            )
        }
    }
}
