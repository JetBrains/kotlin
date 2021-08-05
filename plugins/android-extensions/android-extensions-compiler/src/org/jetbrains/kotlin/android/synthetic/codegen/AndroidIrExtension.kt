/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.synthetic.codegen

import kotlinx.android.extensions.CacheImplementation
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.descriptors.AndroidSyntheticPackageFragmentDescriptor
import org.jetbrains.kotlin.android.synthetic.descriptors.ContainerOptionsProxy
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticFunction
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticProperty
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class AndroidIrExtension : IrGenerationExtension {
    abstract fun isEnabled(declaration: IrClass): Boolean
    abstract fun isExperimental(declaration: IrClass): Boolean
    abstract fun getGlobalCacheImpl(declaration: IrClass): CacheImplementation

    override fun resolveSymbol(symbol: IrSymbol, context: TranslationPluginContext): IrDeclaration? =
        if (symbol !is IrSimpleFunctionSymbol ||
            (symbol.descriptor !is AndroidSyntheticFunction
                    && symbol.descriptor.safeAs<PropertyGetterDescriptor>()?.correspondingProperty !is AndroidSyntheticProperty)) {
            super.resolveSymbol(symbol, context)
        } else {
            // Replace android synthetic functions with stubs, since they are essentially intrinsics and will be replaced in the plugin
            context.declareFunctionStub(symbol.descriptor).also { symbol.bind(it) }
        }

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(AndroidIrTransformer(this, pluginContext), null)
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
private class AndroidIrTransformer(val extension: AndroidIrExtension, val pluginContext: IrPluginContext) :
    IrElementTransformerVoidWithContext() {

    private val cachedPackages = mutableMapOf<FqName, IrPackageFragment>()
    private val cachedClasses = mutableMapOf<FqName, IrClass>()
    private val cachedMethods = mutableMapOf<FqName, IrSimpleFunction>()
    private val cachedFields = mutableMapOf<FqName, IrField>()

    private val cachedCacheFields = mutableMapOf<IrClass, IrField>()
    private val cachedCacheClearFuns = mutableMapOf<IrClass, IrSimpleFunction>()
    private val cachedCacheLookupFuns = mutableMapOf<IrClass, IrSimpleFunction>()

    private val irFactory: IrFactory = IrFactoryImpl

    private fun irBuilder(scope: IrSymbol, replacing: IrStatement): IrBuilderWithScope =
        DeclarationIrBuilder(IrGeneratorContextBase(pluginContext.irBuiltIns), scope, replacing.startOffset, replacing.endOffset)

    private fun createPackage(fqName: FqName) =
        cachedPackages.getOrPut(fqName) {
            IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(pluginContext.moduleDescriptor, fqName)
        }

    private fun createClass(fqName: FqName, isInterface: Boolean = false) =
        cachedClasses.getOrPut(fqName) {
            irFactory.buildClass {
                name = fqName.shortName()
                kind = if (isInterface) ClassKind.INTERFACE else ClassKind.CLASS
                origin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            }.apply {
                parent = createPackage(fqName.parent())
                createImplicitParameterDeclarationWithWrappedDescriptor()
            }
        }

    private fun createMethod(fqName: FqName, type: IrType, inInterface: Boolean = false, f: IrFunction.() -> Unit = {}) =
        cachedMethods.getOrPut(fqName) {
            val parent = createClass(fqName.parent(), inInterface)
            parent.addFunction {
                name = fqName.shortName()
                origin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
                modality = if (inInterface) Modality.ABSTRACT else Modality.FINAL
                returnType = type
            }.apply {
                addDispatchReceiver { this.type = parent.defaultType }
                f()
            }
        }

    private fun createField(fqName: FqName, type: IrType) =
        cachedFields.getOrPut(fqName) {
            createClass(fqName.parent()).addField(fqName.shortName(), type, DescriptorVisibilities.PUBLIC)
        }

    // NOTE: sparse array version intentionally not implemented; this plugin is deprecated
    private val mapFactory = pluginContext.irBuiltIns.findFunctions(Name.identifier("mutableMapOf"), "kotlin", "collections")
        .single { it.descriptor.valueParameters.isEmpty() } // unbound - can't use `owner`
    private val mapGet = pluginContext.irBuiltIns.mapClass.owner.functions
        .single { it.name.asString() == "get" && it.valueParameters.size == 1 }
    private val mapSet = pluginContext.irBuiltIns.mutableMapClass.owner.functions
        .single { it.name.asString() == "put" && it.valueParameters.size == 2 }
    private val mapClear = pluginContext.irBuiltIns.mutableMapClass.owner.functions
        .single { it.name.asString() == "clear" && it.valueParameters.isEmpty() }

    private fun IrClass.getCacheField(): IrField =
        cachedCacheFields.getOrPut(this) {
            val viewType = createClass(FqName(AndroidConst.VIEW_FQNAME)).defaultType.makeNullable()
            irFactory.buildField {
                name = Name.identifier(AbstractAndroidExtensionsExpressionCodegenExtension.PROPERTY_NAME)
                type = pluginContext.irBuiltIns.mutableMapClass.typeWith(pluginContext.irBuiltIns.intType, viewType)
            }.apply {
                parent = this@getCacheField
                initializer = irBuilder(symbol, this).run {
                    irExprBody(irCall(mapFactory, type, valueArgumentsCount = 0, typeArgumentsCount = 2).apply {
                        putTypeArgument(0, context.irBuiltIns.intType)
                        putTypeArgument(1, viewType)
                    })
                }
            }
        }

    private fun IrClass.getClearCacheFun(): IrSimpleFunction =
        cachedCacheClearFuns.getOrPut(this) {
            irFactory.buildFun {
                name = Name.identifier(AbstractAndroidExtensionsExpressionCodegenExtension.CLEAR_CACHE_METHOD_NAME)
                modality = Modality.OPEN
                returnType = pluginContext.irBuiltIns.unitType
            }.apply {
                val self = addDispatchReceiver { type = defaultType }
                parent = this@getClearCacheFun
                body = irBuilder(symbol, this).irBlockBody {
                    +irCall(mapClear).apply { dispatchReceiver = irGetField(irGet(self), getCacheField()) }
                }
            }
        }

    private fun IrClass.getCachedFindViewByIdFun(): IrSimpleFunction =
        cachedCacheLookupFuns.getOrPut(this) {
            val containerType = ContainerOptionsProxy.create(descriptor).containerType
            val viewType = createClass(FqName(AndroidConst.VIEW_FQNAME)).defaultType.makeNullable()
            irFactory.buildFun {
                name = Name.identifier(AbstractAndroidExtensionsExpressionCodegenExtension.CACHED_FIND_VIEW_BY_ID_METHOD_NAME)
                modality = Modality.OPEN
                returnType = viewType
            }.apply {
                val self = addDispatchReceiver { type = defaultType }
                val resourceId = addValueParameter("id", pluginContext.irBuiltIns.intType)
                parent = this@getCachedFindViewByIdFun
                body = irBuilder(symbol, this).irBlockBody {
                    val cache = irTemporary(irGetField(irGet(self), getCacheField()))
                    // cache[resourceId] ?: findViewById(resourceId)?.also { cache[resourceId] = it }
                    +irReturn(irElvis(viewType, irCallOp(mapGet.symbol, viewType, irGet(cache), irGet(resourceId))) {
                        irSafeLet(viewType, irFindViewById(irGet(self), irGet(resourceId), containerType)) { foundView ->
                            irBlock {
                                +irCall(mapSet.symbol).apply {
                                    dispatchReceiver = irGet(cache)
                                    putValueArgument(0, irGet(resourceId))
                                    putValueArgument(1, irGet(foundView))
                                }
                                +irGet(foundView)
                            }
                        }
                    })
                }
            }
        }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (!declaration.isClass && !declaration.isObject)
            return super.visitClassNew(declaration)
        val containerOptions = ContainerOptionsProxy.create(declaration.descriptor)
        if ((containerOptions.cache ?: extension.getGlobalCacheImpl(declaration)) == CacheImplementation.NO_CACHE)
            return super.visitClassNew(declaration)
        if (containerOptions.containerType == AndroidContainerType.LAYOUT_CONTAINER && !extension.isExperimental(declaration))
            return super.visitClassNew(declaration)

        if ((containerOptions.cache ?: extension.getGlobalCacheImpl(declaration)).hasCache) {
            declaration.declarations += declaration.getCacheField()
            declaration.declarations += declaration.getClearCacheFun()
            declaration.declarations += declaration.getCachedFindViewByIdFun()
        }
        return super.visitClassNew(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val receiverClass = expression.extensionReceiver?.type?.classOrNull
            ?: return super.visitFunctionAccess(expression)
        val receiver = expression.extensionReceiver!!.transform(this, null)

        val containerOptions = ContainerOptionsProxy.create(receiverClass.descriptor)
        val containerHasCache = (containerOptions.cache ?: extension.getGlobalCacheImpl(receiverClass.owner)).hasCache

        if (expression.symbol.descriptor is AndroidSyntheticFunction) {
            if (expression.symbol.owner.name.asString() != AndroidConst.CLEAR_FUNCTION_NAME)
                return super.visitFunctionAccess(expression)
            if (!containerHasCache)
                return IrBlockImpl(expression.startOffset, expression.endOffset, expression.type)
            return receiverClass.owner.getClearCacheFun().callWithRanges(expression).apply {
                dispatchReceiver = receiver
            }
        }

        val resource = (expression.symbol.descriptor as? PropertyGetterDescriptor)?.correspondingProperty as? AndroidSyntheticProperty
            ?: return super.visitFunctionAccess(expression)
        val packageFragment = (resource as PropertyDescriptor).containingDeclaration as? AndroidSyntheticPackageFragmentDescriptor
            ?: return super.visitFunctionAccess(expression)

        val packageFqName = FqName(packageFragment.packageData.moduleData.module.applicationPackage)
        val field = createField(packageFqName.child("R\$id").child(resource.name), pluginContext.irBuiltIns.intType)
        val resourceId = IrGetFieldImpl(expression.startOffset, expression.endOffset, field.symbol, field.type)

        val containerType = containerOptions.containerType
        val result = if (expression.type.classifierOrNull?.isFragment == true) {
            // this.get[Support]FragmentManager().findFragmentById(R$id.<name>)
            val appPackageFqName = when (containerType) {
                AndroidContainerType.ACTIVITY,
                AndroidContainerType.FRAGMENT -> FqName("android.app")
                AndroidContainerType.SUPPORT_FRAGMENT,
                AndroidContainerType.SUPPORT_FRAGMENT_ACTIVITY -> FqName("android.support.v4.app")
                AndroidContainerType.ANDROIDX_SUPPORT_FRAGMENT,
                AndroidContainerType.ANDROIDX_SUPPORT_FRAGMENT_ACTIVITY -> FqName("androidx.fragment.app")
                else -> throw IllegalStateException("Invalid Android class type: $this") // Should never occur
            }
            val getFragmentManagerFqName = when (containerType) {
                AndroidContainerType.SUPPORT_FRAGMENT_ACTIVITY,
                AndroidContainerType.ANDROIDX_SUPPORT_FRAGMENT_ACTIVITY -> containerType.fqName.child("getSupportFragmentManager")
                else -> containerType.fqName.child("getFragmentManager")
            }
            val fragment = appPackageFqName.child("Fragment")
            val fragmentManager = appPackageFqName.child("FragmentManager")
            val getFragmentManager = createMethod(getFragmentManagerFqName, createClass(fragmentManager).defaultType)
            createMethod(fragmentManager.child("findFragmentById"), createClass(fragment).defaultType.makeNullable()) {
                addValueParameter("id", pluginContext.irBuiltIns.intType)
            }.callWithRanges(expression).apply {
                dispatchReceiver = getFragmentManager.callWithRanges(expression).apply {
                    dispatchReceiver = receiver
                }
                putValueArgument(0, resourceId)
            }
        } else if (containerHasCache) {
            // this._$_findCachedViewById(R$id.<name>)
            receiverClass.owner.getCachedFindViewByIdFun().callWithRanges(expression).apply {
                dispatchReceiver = receiver
                putValueArgument(0, resourceId)
            }
        } else {
            irBuilder(currentScope!!.scope.scopeOwnerSymbol, expression).irFindViewById(receiver, resourceId, containerType)
        }
        return with(expression) { IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.CAST, type, result) }
    }

    private fun IrBuilderWithScope.irFindViewById(
        receiver: IrExpression, id: IrExpression, container: AndroidContainerType
    ): IrExpression {
        // this[.getView()?|.getContainerView()?].findViewById(R$id.<name>)
        val viewClass = createClass(FqName(AndroidConst.VIEW_FQNAME))
        val getView = when (container) {
            AndroidContainerType.FRAGMENT,
            AndroidContainerType.ANDROIDX_SUPPORT_FRAGMENT,
            AndroidContainerType.SUPPORT_FRAGMENT ->
                createMethod(container.fqName.child("getView"), viewClass.defaultType.makeNullable())
            AndroidContainerType.LAYOUT_CONTAINER ->
                createMethod(container.fqName.child("getContainerView"), viewClass.defaultType.makeNullable(), true)
            else -> null
        }
        val findViewByIdParent = if (getView == null) container.fqName else FqName(AndroidConst.VIEW_FQNAME)
        val findViewById = createMethod(findViewByIdParent.child("findViewById"), viewClass.defaultType.makeNullable()) {
            addValueParameter("id", pluginContext.irBuiltIns.intType)
        }
        val findViewCall = irCall(findViewById).apply { putValueArgument(0, id) }
        return if (getView == null) {
            findViewCall.apply { dispatchReceiver = receiver }
        } else {
            irSafeLet(findViewCall.type, irCall(getView).apply { dispatchReceiver = receiver }) { parent ->
                findViewCall.apply { dispatchReceiver = irGet(parent) }
            }
        }
    }
}

private fun FqName.child(name: String) = child(Name.identifier(name))

private fun IrSimpleFunction.callWithRanges(source: IrExpression) =
    IrCallImpl.fromSymbolOwner(source.startOffset, source.endOffset, returnType, symbol)

private inline fun IrBuilderWithScope.irSafeCall(
    type: IrType, lhs: IrExpression,
    ifNull: IrBuilderWithScope.() -> IrExpression,
    ifNotNull: IrBuilderWithScope.(IrVariable) -> IrExpression
) = irBlock(origin = IrStatementOrigin.SAFE_CALL) {
    +irTemporary(lhs).let { irIfNull(type, irGet(it), ifNull(), ifNotNull(it)) }
}

private inline fun IrBuilderWithScope.irSafeLet(type: IrType, lhs: IrExpression, rhs: IrBuilderWithScope.(IrVariable) -> IrExpression) =
    irSafeCall(type, lhs, { irNull() }, rhs)

private inline fun IrBuilderWithScope.irElvis(type: IrType, lhs: IrExpression, rhs: IrBuilderWithScope.() -> IrExpression) =
    irSafeCall(type, lhs, rhs) { irGet(it) }

private val AndroidContainerType.fqName: FqName
    get() = FqName(internalClassName.replace("/", "."))

private val IrClassifierSymbol.isFragment: Boolean
    get() = isClassWithFqName(FqNameUnsafe(AndroidConst.FRAGMENT_FQNAME)) ||
            isClassWithFqName(FqNameUnsafe(AndroidConst.SUPPORT_FRAGMENT_FQNAME)) ||
            isClassWithFqName(FqNameUnsafe(AndroidConst.ANDROIDX_SUPPORT_FRAGMENT_FQNAME))

private fun TranslationPluginContext.declareTypeParameterStub(typeParameterDescriptor: TypeParameterDescriptor): IrTypeParameter {
    val symbol = IrTypeParameterSymbolImpl(typeParameterDescriptor)
    return irFactory.createTypeParameter(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, symbol, typeParameterDescriptor.name,
        typeParameterDescriptor.index, typeParameterDescriptor.isReified, typeParameterDescriptor.variance
    )
}

private fun TranslationPluginContext.declareParameterStub(parameterDescriptor: ParameterDescriptor): IrValueParameter {
    val symbol = IrValueParameterSymbolImpl(parameterDescriptor)
    val type = typeTranslator.translateType(parameterDescriptor.type)
    val varargElementType = parameterDescriptor.varargElementType?.let { typeTranslator.translateType(it) }
    return irFactory.createValueParameter(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, symbol, parameterDescriptor.name,
        parameterDescriptor.indexOrMinusOne, type, varargElementType, parameterDescriptor.isCrossinline,
        parameterDescriptor.isNoinline, isHidden = false, isAssignable = false
    )
}

private fun TranslationPluginContext.declareFunctionStub(descriptor: FunctionDescriptor): IrSimpleFunction =
    irFactory.buildFun {
        name = descriptor.name
        visibility = descriptor.visibility
        returnType = typeTranslator.translateType(descriptor.returnType!!)
        modality = descriptor.modality
    }.also {
        it.typeParameters = descriptor.propertyIfAccessor.typeParameters.map(this::declareTypeParameterStub)
        it.dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.let(this::declareParameterStub)
        it.extensionReceiverParameter = descriptor.extensionReceiverParameter?.let(this::declareParameterStub)
        it.valueParameters = descriptor.valueParameters.map(this::declareParameterStub)
    }
