/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.stm.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeBuilder
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlinx.stm.compiler.*
import org.jetbrains.kotlinx.stm.compiler.extensions.StmResolveExtension

// Is creating synthetic origin is a good idea or not?
object STM_PLUGIN_ORIGIN : IrDeclarationOriginImpl("STM")

val BackendContext.externalSymbols: ReferenceSymbolTable get() = ir.symbols.externalSymbolTable


interface IrBuilderExtension {
    val compilerContext: IrPluginContext

    val BackendContext.localSymbolTable: SymbolTable

    private fun IrClass.declareSimpleFunctionWithExternalOverrides(descriptor: FunctionDescriptor): IrSimpleFunction {
        return compilerContext.symbolTable.declareSimpleFunction(startOffset, endOffset, STM_PLUGIN_ORIGIN, descriptor)
            .also { f ->
                descriptor.overriddenDescriptors.mapTo(f.overriddenSymbols) {
                    compilerContext.symbolTable.referenceSimpleFunction(it.original)
                }
            }
    }

    fun IrFunction.createParameterDeclarations(
        receiver: IrValueParameter?,
        overwriteValueParameters: Boolean = false
    ) {
        fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            this@createParameterDeclarations.startOffset, this@createParameterDeclarations.endOffset,
            STM_PLUGIN_ORIGIN,
            this,
            type.toIrType(),
            null
        ).also {
            it.parent = this@createParameterDeclarations
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        if (!overwriteValueParameters)
            assert(valueParameters.isEmpty())
        else
            valueParameters.clear()
        valueParameters.addAll(descriptor.valueParameters.map { it.irValueParameter() })

        assert(typeParameters.isEmpty())
    }

    fun IrClass.contributeFunction(
        descriptor: FunctionDescriptor,
        declareNew: Boolean = false,
        bodyGen: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrFunction {
        val f = if (declareNew) declareSimpleFunctionWithExternalOverrides(
            descriptor
        ) else compilerContext.symbolTable.referenceSimpleFunction(descriptor).owner

        f.parent = this
        f.returnType = descriptor.returnType!!.toIrType()
        if (declareNew) f.createParameterDeclarations(this.thisReceiver!!)

        f.body = DeclarationIrBuilder(compilerContext, f.symbol, this.startOffset, this.endOffset).irBlockBody(
            this.startOffset,
            this.endOffset
        ) { bodyGen(f) }

        return f
    }

    fun IrClass.initField(
        f: IrField,
        initGen: IrBuilderWithScope.() -> IrExpression
    ) {
        val builder = DeclarationIrBuilder(compilerContext, f.symbol, this.startOffset, this.endOffset)

        f.initializer = builder.irExprBody(builder.initGen())
    }

    fun IrBuilderWithScope.irInvoke(
        dispatchReceiver: IrExpression? = null,
        callee: IrFunctionSymbol,
        vararg args: IrExpression,
        typeHint: IrType? = null
    ): IrMemberAccessExpression {
        val returnType = typeHint ?: callee.descriptor.returnType!!.toIrType()
        val call = irCall(callee, type = returnType)
        call.dispatchReceiver = dispatchReceiver
        args.forEachIndexed(call::putValueArgument)
        return call
    }

    fun IrBuilderWithScope.irGetObject(classDescriptor: ClassDescriptor) =
        IrGetObjectValueImpl(
            startOffset,
            endOffset,
            classDescriptor.defaultType.toIrType(),
            compilerContext.symbolTable.referenceClass(classDescriptor)
        )

    fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration ->
            compilerContext.symbolTable.withScope(irDeclaration.descriptor) {
                builder(irDeclaration)
            }
        }

    fun KotlinType.toIrType() = compilerContext.typeTranslator.translateType(this)

}

internal fun BackendContext.createTypeTranslator(moduleDescriptor: ModuleDescriptor): TypeTranslator =
    TypeTranslator(externalSymbols, irBuiltIns.languageVersionSettings, moduleDescriptor.builtIns).apply {
        constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable = externalSymbols)
        constantValueGenerator.typeTranslator = this
    }

class STMGenerator(val descr: ClassDescriptor, override val compilerContext: IrPluginContext) : IrBuilderExtension {
    private val _table = SymbolTable()
    override val BackendContext.localSymbolTable: SymbolTable
        get() = _table


    fun generateSTMField(irClass: IrClass, field: IrField, initMethod: IrFunctionSymbol, stmSearcherClass: ClassDescriptor) =
        irClass.initField(field) {
            val obj = irGetObject(stmSearcherClass)
            irCallOp(initMethod, field.type, obj)
        }

    fun createValueParam(
        type: IrType,
        paramDesc: ValueParameterDescriptor,
        name: String,
        index: Int
    ): IrValueParameter {
        val paramSymbol = IrValueParameterSymbolImpl(paramDesc)
        val param = IrValueParameterImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            symbol = paramSymbol,
            name = Name.identifier(name),
            index = index,
            type = type,
            varargElementType = null,
            isCrossinline = false,
            isNoinline = false
        )

        return param
    }

    fun wrapFunctionIntoTransaction(
        irClass: IrClass,
        irFunction: IrSimpleFunction,
        stmField: IrField,
        runAtomically: IrFunctionSymbol,
        stmContextType: IrType,
        atomicFunAnnotation: Annotations
    ) {
        val descriptor = irFunction.descriptor

        irClass.contributeFunction(descriptor) {
            val lambdaDescriptor = AnonymousFunctionDescriptor(
                /*containingDeclaration = */ descriptor,
                /*annotations = */ atomicFunAnnotation,
                /*kind = */ CallableMemberDescriptor.Kind.DECLARATION,
                /*source = */ descriptor.source,
                /*isSuspend = */ descriptor.isSuspend
            )

            val lambdaParamDescr = WrappedValueParameterDescriptor()
            val lambdaParam = createValueParam(stmContextType, lambdaParamDescr, "ctx", index = 0)
            lambdaParamDescr.bind(lambdaParam)

            lambdaDescriptor.initialize(
                descriptor.extensionReceiverParameter,
                descriptor.dispatchReceiverParameter,
                listOf(),
                listOf(lambdaParamDescr),
                descriptor.returnType,
                descriptor.modality,
                Visibilities.DEFAULT_VISIBILITY
            )

            val irLambda = compilerContext.symbolTable.declareSimpleFunction(startOffset, endOffset, STM_PLUGIN_ORIGIN, lambdaDescriptor)
            irLambda.body = irFunction.body

            irLambda.dispatchReceiverParameter = lambdaParam
            lambdaParam.parent = irLambda

            irLambda.returnType = irFunction.returnType

            irLambda.patchDeclarationParents(irFunction)

            val lambdaType = runAtomically.descriptor.valueParameters[1].type.toIrType()

            val lambdaExpression = IrFunctionExpressionImpl(
                irLambda.startOffset, irLambda.endOffset,
                lambdaType,
                irLambda,
                IrStatementOrigin.LAMBDA
            )

            val stmFieldExpr = irGetField(irGet(irFunction.dispatchReceiverParameter!!), stmField)

            +irReturn(
                irInvoke(
                    dispatchReceiver = stmFieldExpr,
                    callee = runAtomically,
                    args = *arrayOf(irNull(), lambdaExpression),
                    typeHint = irFunction.returnType
                ).apply {
                    putTypeArgument(index = 0, type = irFunction.returnType)
                }
            )
        }
    }

    fun addDelegateField(
        irClass: IrClass,
        propertyName: Name,
        backingField: IrField,
        stmField: IrField,
        wrap: IrFunctionSymbol,
        universalDelegateClassSymbol: IrClassSymbol
    ): IrField {

        val delegateType = IrSimpleTypeBuilder().run {
            classifier = universalDelegateClassSymbol
            hasQuestionMark = false
            val type = backingField.type

            val outSTMContext = wrap.descriptor.returnType?.arguments?.get(1)
                ?: throw StmLoweringException("Couldn't find wrap function in the STM runtime library")

            arguments = listOf(
                makeTypeProjection(type, Variance.INVARIANT),
                makeTypeProjection(outSTMContext.type.toIrType(), Variance.OUT_VARIANCE)
            )
            buildSimpleType()
        }

        val delegateField = irClass.addField {
            name = Name.identifier("${propertyName}${SHARABLE_NAME_SUFFIX}")
            type = delegateType
            visibility = Visibilities.PRIVATE
            origin = IrDeclarationOrigin.DELEGATED_MEMBER
            isFinal = true
            isStatic = false
            buildField()
        }

        irClass.initField(delegateField) {
            val stmFieldExpr = irGetField(irGet(irClass.thisReceiver!!), stmField)

            val initValue = backingField.initializer?.expression ?: irNull(backingField.type)

            irInvoke(
                dispatchReceiver = stmFieldExpr,
                callee = wrap,
                args = *arrayOf(initValue),
                typeHint = delegateField.type
            ).apply {
                putTypeArgument(index = 0, type = backingField.type)
            }
        }

        return delegateField
    }

    fun addGetFunction(
        irClass: IrClass,
        propertyName: Name,
        delegate: IrField,
        stmField: IrField,
        getVar: IrFunctionSymbol
    ): IrFunction? {
        val getterFunDescriptor =
            irClass.descriptor.findMethods(StmResolveExtension.getterName(propertyName)).firstOrNull() ?: return null

        return irClass.contributeFunction(getterFunDescriptor, declareNew = true) { f ->
            val stmFieldExpr = irGetField(irGet(f.dispatchReceiverParameter!!), stmField) // TODO: koshka

            val stmContextParam = f.valueParameters[0]

            val delegateFieldExpr = irGetField(irGet(f.dispatchReceiverParameter!!), delegate)

            +irReturn(
                irInvoke(
                    dispatchReceiver = stmFieldExpr,
                    callee = getVar,
                    args = *arrayOf(irGet(stmContextParam), delegateFieldExpr),
                    typeHint = getterFunDescriptor.returnType?.toIrType()
                ).apply {
                    putTypeArgument(index = 0, type = delegateFieldExpr.type.safeAs<IrSimpleType>()?.arguments?.first()?.typeOrNull)
                }
            )
        }
    }

    fun addSetFunction(
        irClass: IrClass,
        propertyName: Name,
        delegate: IrField,
        stmField: IrField,
        setVar: IrFunctionSymbol
    ): IrFunction? {
        val setterFunDescriptor =
            irClass.descriptor.findMethods(StmResolveExtension.setterName(propertyName)).firstOrNull() ?: return null

        return irClass.contributeFunction(setterFunDescriptor, declareNew = true) { f ->
            val stmFieldExpr = irGetField(irGet(f.dispatchReceiverParameter!!), stmField)

            val stmContextParam = f.valueParameters[0]
            val newValueParameter = f.valueParameters[1]

            val delegateFieldExpr = irGetField(irGet(f.dispatchReceiverParameter!!), delegate)

            irInvoke(
                dispatchReceiver = stmFieldExpr,
                callee = setVar,
                args = *arrayOf(irGet(stmContextParam), delegateFieldExpr, irGet(newValueParameter)),
                typeHint = setterFunDescriptor.returnType?.toIrType()
            ).apply {
                putTypeArgument(index = 0, type = newValueParameter.type)
            }
        }
    }

    fun addDelegateAndAccessorFunctions(
        irClass: IrClass,
        propertyName: Name,
        backingField: IrField,
        stmField: IrField,
        wrap: IrFunctionSymbol,
        universalDelegateClassSymbol: IrClassSymbol,
        getVar: IrFunctionSymbol,
        setVar: IrFunctionSymbol
    ): List<IrFunction> {
        val delegate =
            addDelegateField(irClass, propertyName, backingField, stmField, wrap, universalDelegateClassSymbol)

        val result = mutableListOf<IrFunction>()

        val getterFun = addGetFunction(irClass, propertyName, delegate, stmField, getVar)
        val setterFun = addSetFunction(irClass, propertyName, delegate, stmField, setVar)

        if (getterFun != null)
            result += getterFun
        if (setterFun != null)
            result += setterFun

        return result
    }
}

private fun ClassDescriptor.checkPublishMethodResult(type: KotlinType): Boolean =
    KotlinBuiltIns.isInt(type)

private fun ClassDescriptor.checkPublishMethodParameters(parameters: List<ValueParameterDescriptor>): Boolean =
    parameters.size == 0

class StmLoweringException(override val message: String) : Exception()

open class StmIrGenerator {

    companion object {

        private fun findSTMClassDescriptorOrThrow(module: ModuleDescriptor, symbolTable: SymbolTable, className: Name): ClassDescriptor =
            module.findClassAcrossModuleDependencies(
                ClassId(
                    STM_PACKAGE,
                    className
                )
            ) ?: throw StmLoweringException("Couldn't find $className runtime class in dependencies of module ${module.name}")

        private fun findMethodDescriptorOrThrow(
            module: ModuleDescriptor,
            classDescriptor: ClassDescriptor,
            methodName: Name
        ): SimpleFunctionDescriptor =
            classDescriptor.findMethods(methodName).firstOrNull()
                ?: throw StmLoweringException(
                    "Couldn't find ${classDescriptor.name}.$methodName(...) runtime method in dependencies of module ${module.name}"
                )

        private fun findSTMMethodDescriptorOrThrow(
            module: ModuleDescriptor,
            symbolTable: SymbolTable,
            className: Name,
            methodName: Name
        ): SimpleFunctionDescriptor = findMethodDescriptorOrThrow(
            module,
            findSTMClassDescriptorOrThrow(module, symbolTable, className),
            methodName
        )

        private fun findSTMMethodIrOrThrow(
            module: ModuleDescriptor,
            symbolTable: SymbolTable,
            className: Name,
            methodName: Name
        ): IrFunctionSymbol =
            symbolTable.referenceSimpleFunction(findSTMMethodDescriptorOrThrow(module, symbolTable, className, methodName))

        private fun getSTMField(irClass: IrClass, symbolTable: SymbolTable): IrField {
            val stmClassSymbol = findSTMClassDescriptorOrThrow(irClass.module, symbolTable, STM_INTERFACE)
                .let(symbolTable::referenceClass)

            val stmType = IrSimpleTypeBuilder().run {
                classifier = stmClassSymbol
                hasQuestionMark = false
                buildSimpleType()
            }

            return irClass.addField {
                name = Name.identifier(STM_FIELD_NAME)
                type = stmType
                visibility = Visibilities.PRIVATE
                origin = IrDeclarationOrigin.DELEGATED_MEMBER
                isFinal = true
                isStatic = false
                buildField()
            }
        }

        private fun getSTMSearchMethod(module: ModuleDescriptor, symbolTable: SymbolTable): IrFunctionSymbol =
            findSTMMethodIrOrThrow(module, symbolTable, STM_SEARCHER, SEARCH_STM_METHOD)

        private fun getSTMWrapMethod(module: ModuleDescriptor, symbolTable: SymbolTable): IrFunctionSymbol =
            findSTMMethodIrOrThrow(module, symbolTable, STM_INTERFACE, WRAP_METHOD)

        private fun getSTMGetvarMethod(module: ModuleDescriptor, symbolTable: SymbolTable): IrFunctionSymbol =
            findSTMMethodIrOrThrow(module, symbolTable, STM_INTERFACE, GET_VAR_METHOD)

        private fun getSTMSetvarMethod(module: ModuleDescriptor, symbolTable: SymbolTable): IrFunctionSymbol =
            findSTMMethodIrOrThrow(module, symbolTable, STM_INTERFACE, SET_VAR_METHOD)

        private fun getSTMSearchClass(module: ModuleDescriptor, symbolTable: SymbolTable): ClassDescriptor =
            findSTMClassDescriptorOrThrow(module, symbolTable, STM_SEARCHER)

        private fun getRunAtomicallyFun(module: ModuleDescriptor, symbolTable: SymbolTable): IrFunctionSymbol =
            findSTMMethodIrOrThrow(module, symbolTable, STM_INTERFACE, RUN_ATOMICALLY_METHOD)

        private fun getSTMContextType(compilerContext: IrPluginContext, module: ModuleDescriptor, symbolTable: SymbolTable): IrType =
            compilerContext.typeTranslator.translateType(
                findSTMMethodIrOrThrow(module, symbolTable, STM_INTERFACE, GET_CONTEXT).descriptor.returnType!!
            )

        fun patchSharedClass(
            irClass: IrClass,
            context: IrPluginContext,
            symbolTable: SymbolTable
        ) {
            val generator = STMGenerator(irClass.descriptor, context)

            val stmField = getSTMField(irClass, symbolTable)
            val stmSearch = getSTMSearchMethod(irClass.module, symbolTable)
            val stmSearchClass = getSTMSearchClass(irClass.module, symbolTable)
            generator.generateSTMField(irClass, stmField, stmSearch, stmSearchClass)

            val universalDelegateClassSymbol = findSTMClassDescriptorOrThrow(irClass.module, symbolTable, UNIVERSAL_DELEGATE)
                .let(symbolTable::referenceClass)
            val stmWrap = getSTMWrapMethod(irClass.module, symbolTable)
            val getVar = getSTMGetvarMethod(irClass.module, symbolTable)
            val setVar = getSTMSetvarMethod(irClass.module, symbolTable)
            val stmContextType = getSTMContextType(context, irClass.module, symbolTable)

            val runAtomically = getRunAtomicallyFun(irClass.module, symbolTable)

            val atomicFunAnnotation = runAtomically.descriptor.valueParameters[1].type.annotations

            irClass.functions.forEach { f ->
                generator.wrapFunctionIntoTransaction(irClass, f, stmField, runAtomically, stmContextType, atomicFunAnnotation)
            }


            irClass.transformDeclarationsFlat { p ->
                when (p) {
                    is IrProperty -> {
                        val backingField = p.backingField
                        val pName = p.name

                        if (backingField != null && !pName.isSTMFieldName() && !pName.isSharable())
                            generator.addDelegateAndAccessorFunctions(
                                irClass,
                                pName,
                                backingField,
                                stmField,
                                stmWrap,
                                universalDelegateClassSymbol,
                                getVar,
                                setVar
                            )
                        else
                            null
                    }
                    is IrField -> {
                        val pName = p.name

                        if (pName.isSTMFieldName() || pName.isSharable())
                            null
                        else
                            generator.addDelegateAndAccessorFunctions(
                                irClass,
                                pName,
                                p,
                                stmField,
                                stmWrap,
                                universalDelegateClassSymbol,
                                getVar,
                                setVar
                            )
                    }
                    else -> null
                }
            }

            println("KOKO")
        }

        private fun getSyntheticAccessorForSharedClass(
            module: ModuleDescriptor,
            symbolTable: SymbolTable,
            classDescriptor: ClassDescriptor,
            accessorName: Name
        ): IrFunctionSymbol =
            symbolTable.referenceSimpleFunction(
                findMethodDescriptorOrThrow(
                    module,
                    classDescriptor,
                    accessorName
                )
            )

        private fun getSyntheticGetterForSharedClass(
            module: ModuleDescriptor,
            symbolTable: SymbolTable,
            classDescriptor: ClassDescriptor,
            varName: Name
        ): IrFunctionSymbol =
            getSyntheticAccessorForSharedClass(module, symbolTable, classDescriptor, StmResolveExtension.getterName(varName))


        private fun getSyntheticSetterForSharedClass(
            module: ModuleDescriptor,
            symbolTable: SymbolTable,
            classDescriptor: ClassDescriptor,
            varName: Name
        ): IrFunctionSymbol =
            getSyntheticAccessorForSharedClass(module, symbolTable, classDescriptor, StmResolveExtension.setterName(varName))

        fun patchPropertyAccess(
            irCall: IrCall,
            accessorDescriptor: PropertyAccessorDescriptor,
            functionStack: MutableList<IrFunction>,
            symbolTable: SymbolTable
        ): IrCall {
            // TODO: support (extension) property access with no backing field!
            // In this case we must not substitute getter/setter with synthetic members!

            val propertyName = accessorDescriptor.correspondingProperty.name

            if (propertyName.asString().startsWith(STM_FIELD_NAME))
                return irCall

            val dispatchReceiver = irCall.dispatchReceiver?.deepCopyWithVariables()
            val extensionReceiver = irCall.extensionReceiver?.deepCopyWithVariables()
            val classDescriptor = dispatchReceiver?.type?.classOrNull?.descriptor
                ?: extensionReceiver?.type?.classOrNull?.descriptor
                ?: throw StmLoweringException("Unexpected call of setter for an unknown class (setter's descriptor could not be found: $irCall)")

            fun isStmContextType(type: IrType?) = type?.classOrNull?.isClassWithFqName(STM_CONTEXT_CLASS.toUnsafe()) ?: false

            val ctx = functionStack.firstNotNullResult {
                when {
                    isStmContextType(it.dispatchReceiverParameter?.type) -> {
                        it.dispatchReceiverParameter!!
                    }
                    isStmContextType(it.extensionReceiverParameter?.type) -> {
                        it.extensionReceiverParameter!!
                    }
                    isStmContextType(it.valueParameters.lastOrNull()?.type) -> {
                        it.valueParameters.last()
                    }
                    else -> null
                }
            }
                ?: throw StmLoweringException("Call of function ${accessorDescriptor.name} requires $STM_CONTEXT_CLASS to be present in scope")

            val contextValue = IrGetValueImpl(ctx.startOffset, ctx.endOffset, ctx.type, ctx.symbol)

            fun callFunction(f: IrFunctionSymbol, vararg args: IrExpression?): IrCall {
                val call = IrCallImpl(
                    irCall.startOffset,
                    irCall.endOffset,
                    irCall.type,
                    f,
                    irCall.origin,
                    irCall.superQualifierSymbol
                )

                args.forEachIndexed { index, irExpression -> call.putValueArgument(index, irExpression) }

                call.dispatchReceiver = dispatchReceiver
                call.extensionReceiver = extensionReceiver

                return call
            }

            return when {
                accessorDescriptor.name.asString().startsWith(KT_DEFAULT_GET_PREFIX) -> callFunction(
                    getSyntheticGetterForSharedClass(accessorDescriptor.module, symbolTable, classDescriptor, propertyName),
                    args = *arrayOf(contextValue)
                )
                else -> /* KT_DEFAULT_SET_PREFIX */ {
                    val newValue = irCall.getValueArgument(0)?.deepCopyWithVariables()

                    callFunction(
                        getSyntheticSetterForSharedClass(accessorDescriptor.module, symbolTable, classDescriptor, propertyName),
                        args = *arrayOf(contextValue, newValue)
                    )
                }
            }

        }

        fun patchFunction(irFunction: IrFunction, compilerContext: IrPluginContext, symbolTable: SymbolTable): IrFunction {
            val stmContextType = getSTMContextType(compilerContext, irFunction.module, symbolTable)

            irFunction.valueParameters += IrValueParameterImpl(
                irFunction.startOffset,
                irFunction.endOffset,
                irFunction.origin,
                ValueParameterDescriptorImpl(
                    containingDeclaration = irFunction.descriptor,
                    original = null,
                    index = irFunction.valueParameters.size,
                    annotations = Annotations.EMPTY,
                    name = Name.identifier("ctx"),
                    outType = stmContextType.toKotlinType(),
                    declaresDefaultValue = false,
                    isCrossinline = false,
                    isNoinline = false,
                    varargElementType = null,
                    source = SourceElement.NO_SOURCE
                ),
                type = stmContextType,
                varargElementType = null
            ).also { it.parent = irFunction }

            return irFunction
        }
    }
}

//@Shared
//fun f() {
//
//}

//fun Context.f2() {
//    x.setZ(ctx, 1)
//}