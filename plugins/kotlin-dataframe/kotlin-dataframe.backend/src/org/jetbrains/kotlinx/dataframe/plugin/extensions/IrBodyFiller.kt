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
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
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

class IrBodyFiller(
    private val handleExtensionPropertyExceptionId: CallableId? = HANDLE_EXTENSION_PROPERTY_EXCEPTION_ID,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        DataFrameFileLowering(pluginContext, handleExtensionPropertyExceptionId).lower(moduleFragment)
        IrImportedSchemaGenerator(pluginContext).lower(moduleFragment)
    }

    companion object {
        val HANDLE_EXTENSION_PROPERTY_EXCEPTION_ID: CallableId = CallableId(
            FqName("org.jetbrains.kotlinx.dataframe.exceptions"),
            Name.identifier("handleExtensionPropertyException"),
        )
    }
}

private class DataFrameFileLowering(
    val context: IrPluginContext,
    val handleExtensionPropertyExceptionId: CallableId?,
) : FileLoweringPass,
    IrTransformer<IrDeclarationParent>() {
    companion object {
        val COLUMNS_SCOPE_ID =
            CallableId(ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("ColumnsScope")), Name.identifier("get"))
        val DATA_ROW_ID =
            CallableId(ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("DataRow")), Name.identifier("get"))
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, irFile)
    }

    override fun visitConstructor(declaration: IrConstructor, data: IrDeclarationParent): IrStatement {
        val origin = declaration.origin
        if (!(origin is IrDeclarationOrigin.GeneratedByPlugin && (origin.pluginKey is DataFrameTokenContentKey || origin.pluginKey is DataFramePlugin))) return declaration
        declaration.body = generateBodyForDefaultConstructor(context, declaration)
        return declaration
    }

    override fun visitFunction(declaration: IrFunction, data: IrDeclarationParent): IrStatement {
        declaration.transformChildren(this, declaration)
        return declaration
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitProperty(declaration: IrProperty, data: IrDeclarationParent): IrStatement {
        val origin = declaration.origin
        val pluginKey = (origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey as? DataFramePlugin
        if (pluginKey == null || declaration.modality == Modality.ABSTRACT) {
            declaration.transformChildren(this, data)
            return declaration
        }
        val getter = declaration.getter ?: return declaration

        val getterExtensionReceiver = getter.parameters.single { it.kind == IrParameterKind.ExtensionReceiver }
        val marker = ((getterExtensionReceiver.type as IrSimpleType).arguments.single() as IrSimpleType).classOrFail.owner

        val returnType = getter.returnType
        val typeOp = generateColumnAccessCall(
            receiver = IrGetValueImpl(-1, -1, getterExtensionReceiver.symbol), declaration, returnType, marker, data = getter
        )
        val returnExpression = IrReturnImpl(-1, -1, context.irBuiltIns.nothingType, getter.symbol, typeOp)
        getter.apply {
            body = factory.createBlockBody(-1, -1, listOf(returnExpression))
        }

        return declaration
    }

    /**
     * Generate a catch block for column accessor that delegates to
     * [org.jetbrains.kotlinx.dataframe.exceptions.handleExtensionPropertyException],
     * which throws an [IllegalStateException] with a custom message depending on the original exception type.
     */
    private fun generateColumnAccessCatchBlock(
        context: IrPluginContext,
        catchParameter: IrVariable,
        columnName: String,
        handleFn: IrSimpleFunctionSymbol,
    ): IrBlock {

        // handleExtensionPropertyException(e, columnName)
        val handleCall = IrCallImpl(
            -1, -1, context.irBuiltIns.unitType, handleFn, 0
        ).also {
            it.arguments[0] = IrGetValueImpl(-1, -1, catchParameter.symbol)
            it.arguments[1] = IrConstImpl.string(-1, -1, context.irBuiltIns.stringType, columnName)
        }

        return IrBlockImpl(
            -1, -1, context.irBuiltIns.nothingType,
            statements = listOf(handleCall),
        )
    }

    private fun generateColumnAccessCall(
        receiver: IrExpression,
        property: IrProperty,
        returnType: IrType,
        marker: IrClass?,
        data: IrDeclarationParent,
    ): IrExpression {
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

        val columnName = marker?.properties?.firstOrNull { it.name == property.name }
            ?.getAnnotationArgumentValue<String>(Names.COLUMN_NAME_ANNOTATION.asSingleFqName(), Names.COLUMN_NAME_ARGUMENT.identifier)
            ?: property.name.identifier

        val call = IrCallImpl(-1, -1, context.irBuiltIns.anyNType, get, 0).also {
            it.arguments[0] = receiver
            it.arguments[1] = IrConstImpl.string(-1, -1, context.irBuiltIns.stringType, columnName)
        }

        // cast expression
        // this["name"] as Type
        val cast = IrTypeOperatorCallImpl(-1, -1, returnType, IrTypeOperator.CAST, returnType, call)

        // try to find handleExtensionPropertyException
        // not found (for older KDF versions) -> just return cast expression;
        // otherwise wrap it into try/catch
        val handleFn = handleExtensionPropertyExceptionId
            ?.let { finder.findFunctions(it).singleOrNull() }
            ?: return cast

        val exceptionClass = finder.findClass(ClassId(FqName("kotlin"), Name.identifier("Exception")))!!

        // e: Exception
        val catchParameter = buildVariable(
            parent = data,
            startOffset = -1,
            endOffset = -1,
            origin = IrDeclarationOrigin.CATCH_PARAMETER,
            name = Name.identifier("e"),
            type = exceptionClass.defaultType,
        )

        // exceptions handling inside catch { ... }
        val catchBlock = generateColumnAccessCatchBlock(context, catchParameter, columnName, handleFn)

        // cast block inside try { ... }
        val tryCastBlock = IrBlockImpl(-1, -1, returnType, statements = listOf(cast))

        // single catch(e: Exception) { catchBlock }
        val catchExpression = IrCatchImpl(-1, -1, catchParameter, result = catchBlock)

        // try { castBlock } catch(e: Exception) { catchBlock }
        val tryExpression = IrTryImpl(-1, -1, returnType, tryCastBlock, listOf(catchExpression), null)

        return tryExpression
    }

    // Implicit receivers injected by org.jetbrains.kotlinx.dataframe.plugin.extensions.ReturnTypeBasedReceiverInjector
    // don't "exist": they are used for resolve, but there's no value on the stack.
    // We need to find all calls that use them as arguments and generate valid code
    // region injected receivers lowering
    override fun visitCall(expression: IrCall, data: IrDeclarationParent): IrElement {
        val origin = expression.symbol.owner.origin
        if (expression.origin == IrStatementOrigin.GET_PROPERTY && origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey == DataFramePlugin) {
            val receiverType = expression.symbol.owner.parameters.getOrNull(0)?.type
            if (receiverType != null) {
                if (receiverType.isScope()) {
                    val constructor = receiverType.classOrFail.constructors.single()
                    expression.arguments[0] = IrConstructorCallImpl(-1, -1, receiverType, constructor, 0, 0)
                } else if (expression.arguments.size == 1) {
                    return inlineExtensionProperty(receiverType, extensionPropertyCall = expression, data = data)
                }

            }
            return super.visitCall(expression, data)
        }
        return super.visitCall(expression, data)
    }

    private fun inlineExtensionProperty(
        receiverType: IrType,
        extensionPropertyCall: IrCall,
        data: IrDeclarationParent,
    ): IrExpression = generateColumnAccessCall(
        receiver = extensionPropertyCall.arguments.getOrNull(0)?.transform(this, data)
            ?: errorWithAttachment("Generated property with unexpected shape") {
                withEntry("Expression", extensionPropertyCall) { it.render() }
                withEntry("Symbol", extensionPropertyCall.symbol.owner) { it.render() }
            },
        extensionPropertyCall.symbol.owner.correspondingPropertySymbol!!.owner,
        extensionPropertyCall.type,
        marker = ((receiverType as IrSimpleType).arguments[0] as IrSimpleType).classOrFail.owner,
        data = data
    )

    private fun IrType.isScope(): Boolean {
        val origin = (classifierOrNull?.owner as? IrClass)?.origin ?: return false
        val fromPlugin = origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey is DataFramePlugin
        val scopeReference = classFqName?.shortName()?.asString()?.startsWith("Scope") ?: false
        return fromPlugin || scopeReference
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: IrDeclarationParent): IrExpression {
        if (expression.type.isScope()) {
            return expression.replaceWithConstructorCall()
        }
        return super.visitErrorCallExpression(expression, data)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: IrDeclarationParent): IrExpression {
        if (expression.type.isScope() &&
            expression.operator == IrTypeOperator.IMPLICIT_CAST &&
            expression.argument is IrGetValue
        ) {
            return expression.replaceWithConstructorCall()
        }
        return super.visitTypeOperator(expression, data)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrExpression.replaceWithConstructorCall(): IrConstructorCallImpl {
        val constructor = type.getClass()!!.constructors.toList().single()
        return IrConstructorCallImpl(-1, -1, type, constructor.symbol, 0, 0)
    }
    // endregion
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
