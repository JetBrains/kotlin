/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.sourceElement
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.typeConstructor
import org.jetbrains.kotlin.types.model.dependsOnTypeConstructor
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.serialization.compiler.backend.common.*
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.*
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationDependencies.FUNCTION0_FQ
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationDependencies.LAZY_FQ
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationDependencies.LAZY_FUNC_FQ
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationDependencies.LAZY_MODE_FQ
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationDependencies.LAZY_PUBLICATION_MODE_NAME

interface IrBuilderExtension {
    val compilerContext: SerializationPluginContext

    private val throwMissedFieldExceptionFunc
        get() = compilerContext.referenceFunctions(CallableId(SerializationPackages.internalPackageFqName, SerialEntityNames.SINGLE_MASK_FIELD_MISSING_FUNC_NAME)).singleOrNull()

    private val throwMissedFieldExceptionArrayFunc
        get() = compilerContext.referenceFunctions(CallableId(SerializationPackages.internalPackageFqName, SerialEntityNames.ARRAY_MASK_FIELD_MISSING_FUNC_NAME)).singleOrNull()

    private val enumSerializerFactoryFunc
        get() = compilerContext.referenceFunctions(CallableId(SerializationPackages.internalPackageFqName, SerialEntityNames.ENUM_SERIALIZER_FACTORY_FUNC_NAME)).singleOrNull()

    private val markedEnumSerializerFactoryFunc
        get() = compilerContext.referenceFunctions(CallableId(SerializationPackages.internalPackageFqName, SerialEntityNames.MARKED_ENUM_SERIALIZER_FACTORY_FUNC_NAME)).singleOrNull()

    private inline fun <reified T : IrDeclaration> IrClass.searchForDeclaration(descriptor: DeclarationDescriptor): T? {
        return declarations.singleOrNull { it.descriptor == descriptor } as? T
    }

    fun useFieldMissingOptimization(): Boolean {
        return throwMissedFieldExceptionFunc != null && throwMissedFieldExceptionArrayFunc != null
    }

    fun <F: IrFunction> addFunctionBody(function: F, bodyGen: IrBlockBodyBuilder.(F) -> Unit) {
        function.body = DeclarationIrBuilder(compilerContext, function.symbol, function.startOffset, function.endOffset).irBlockBody(
            function.startOffset,
            function.endOffset
        ) { bodyGen(function) }
    }

    fun IrClass.contributeFunction(descriptor: FunctionDescriptor, ignoreWhenMissing: Boolean = false, bodyGen: IrBlockBodyBuilder.(IrFunction) -> Unit) {
        val f: IrSimpleFunction = searchForDeclaration(descriptor)
            ?: (if (ignoreWhenMissing) return else compilerContext.symbolTable.referenceSimpleFunction(descriptor).owner)
        f.body = DeclarationIrBuilder(compilerContext, f.symbol, this.startOffset, this.endOffset).irBlockBody(
            this.startOffset,
            this.endOffset
        ) { bodyGen(f) }
    }

    fun IrClass.contributeConstructor(
        descriptor: ClassConstructorDescriptor,
        declareNew: Boolean = true,
        overwriteValueParameters: Boolean = false,
        bodyGen: IrBlockBodyBuilder.(IrConstructor) -> Unit
    ) {
        val c: IrConstructor = searchForDeclaration(descriptor) ?: compilerContext.symbolTable.referenceConstructor(descriptor).owner
        c.body = DeclarationIrBuilder(compilerContext, c.symbol, this.startOffset, this.endOffset).irBlockBody(
            this.startOffset,
            this.endOffset
        ) { bodyGen(c) }
    }

    fun IrClass.createLambdaExpression(
        type: IrType,
        bodyGen: IrBlockBodyBuilder.() -> Unit
    ): IrFunctionExpression {
        val function = compilerContext.irFactory.buildFun {
            this.startOffset = this@createLambdaExpression.startOffset
            this.endOffset = this@createLambdaExpression.endOffset
            this.returnType = type
            name = Name.identifier("<anonymous>")
            visibility = DescriptorVisibilities.LOCAL
            origin = SERIALIZABLE_PLUGIN_ORIGIN
        }
        function.body =
            DeclarationIrBuilder(compilerContext, function.symbol, startOffset, endOffset).irBlockBody(startOffset, endOffset, bodyGen)
        function.parent = this

        val f0Type = compilerContext.irBuiltIns.functionN(0)
        val f0ParamSymbol = f0Type.typeParameters[0].symbol
        val f0IrType = f0Type.defaultType.substitute(mapOf(f0ParamSymbol to type))

        return IrFunctionExpressionImpl(
            startOffset,
            endOffset,
            f0IrType,
            function,
            IrStatementOrigin.LAMBDA
        )
    }

    fun createLazyProperty(
        containingClass: IrClass,
        targetIrType: IrType,
        name: Name,
        initializerBuilder: IrBlockBodyBuilder.() -> Unit
    ): IrProperty {
        val lazySafeModeClassDescriptor = compilerContext.referenceClass(ClassId.topLevel(LAZY_MODE_FQ))!!.owner
        val lazyFunctionSymbol = compilerContext.referenceFunctions(CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("lazy"))).single {
            it.descriptor.valueParameters.size == 2 && it.descriptor.valueParameters[0].type == lazySafeModeClassDescriptor.defaultType
        }
        val publicationEntryDescriptor = lazySafeModeClassDescriptor.enumEntries().single { it.name == LAZY_PUBLICATION_MODE_NAME }

        val lazyIrClass = compilerContext.referenceClass(ClassId.topLevel(LAZY_FQ))!!.owner
        val lazyIrType = lazyIrClass.defaultType.substitute(mapOf(lazyIrClass.typeParameters[0].symbol to targetIrType))

        val propertyDescriptor =
            KSerializerDescriptorResolver.createValPropertyDescriptor(
                Name.identifier(name.asString() + "\$delegate"),
                containingClass.descriptor,
                lazyIrType.toKotlinType(),
                createGetter = true
            )

        return generateSimplePropertyWithBackingField(propertyDescriptor, containingClass).apply {
            val builder = DeclarationIrBuilder(compilerContext, containingClass.symbol, startOffset, endOffset)
            val initializerBody = builder.run {
                val enumElement = IrGetEnumValueImpl(
                    startOffset,
                    endOffset,
                    lazySafeModeClassDescriptor.defaultType,
                    publicationEntryDescriptor.symbol
                )

                val lambdaExpression = containingClass.createLambdaExpression(targetIrType, initializerBuilder)

                irExprBody(
                    irInvoke(null, lazyFunctionSymbol, listOf(targetIrType), listOf(enumElement, lambdaExpression), lazyIrType)
                )
            }
            backingField!!.initializer = initializerBody
        }
    }

    fun createCompanionValProperty(
        companionClass: IrClass,
        type: IrType,
        name: Name,
        initializerBuilder: IrBlockBodyBuilder.() -> Unit
    ): IrProperty {
        val targetKotlinType = type.toKotlinType()
        val propertyDescriptor =
            KSerializerDescriptorResolver.createValPropertyDescriptor(name, companionClass.descriptor, targetKotlinType)

        return generateSimplePropertyWithBackingField(propertyDescriptor, companionClass, name).apply {
            companionClass.contributeAnonymousInitializer {
                val irBlockBody = irBlockBody(startOffset, endOffset, initializerBuilder)
                irBlockBody.statements.dropLast(1).forEach { +it }
                val expression = irBlockBody.statements.last() as? IrExpression
                    ?: throw AssertionError("Last statement in property initializer builder is not an a expression")
                +irSetField(irGetObject(companionClass), backingField!!, expression)
            }
        }
    }

    fun IrClass.contributeAnonymousInitializer(bodyGen: IrBlockBodyBuilder.() -> Unit) {
        val symbol = IrAnonymousInitializerSymbolImpl(descriptor)
        factory.createAnonymousInitializer(startOffset, endOffset, SERIALIZABLE_PLUGIN_ORIGIN, symbol).also {
            it.parent = this
            declarations.add(it)
            it.body = DeclarationIrBuilder(compilerContext, symbol, startOffset, endOffset).irBlockBody(startOffset, endOffset, bodyGen)
        }
    }

    fun IrBlockBodyBuilder.getLazyValueExpression(thisParam: IrValueParameter, property: IrProperty, type: IrType): IrExpression {
        val lazyIrClass = compilerContext.referenceClass(ClassId.topLevel(LAZY_FQ))!!.owner
        val valueGetter = lazyIrClass.getPropertyGetter("value")!!

        val propertyGetter = property.getter!!

        return irInvoke(
            irGet(propertyGetter.returnType, irGet(thisParam), propertyGetter.symbol),
            valueGetter,
            typeHint = type
        )
    }

    fun IrBuilderWithScope.irInvoke(
        dispatchReceiver: IrExpression? = null,
        callee: IrFunctionSymbol,
        vararg args: IrExpression,
        typeHint: IrType? = null
    ): IrMemberAccessExpression<*> {
        assert(callee.isBound) { "Symbol $callee expected to be bound" }
        val returnType = typeHint ?: callee.owner.returnType
        val call = irCall(callee, type = returnType)
        call.dispatchReceiver = dispatchReceiver
        args.forEachIndexed(call::putValueArgument)
        return call
    }

    fun IrBuilderWithScope.irInvoke(
        dispatchReceiver: IrExpression? = null,
        callee: IrFunctionSymbol,
        typeArguments: List<IrType?>,
        valueArguments: List<IrExpression>,
        returnTypeHint: IrType? = null
    ): IrMemberAccessExpression<*> =
        irInvoke(
            dispatchReceiver,
            callee,
            *valueArguments.toTypedArray(),
            typeHint = returnTypeHint
        ).also { call -> typeArguments.forEachIndexed(call::putTypeArgument) }

    fun IrBuilderWithScope.createArrayOfExpression(
        arrayElementType: IrType,
        arrayElements: List<IrExpression>
    ): IrExpression {

        val arrayType = compilerContext.irBuiltIns.arrayClass.typeWith(arrayElementType)
        val arg0 = IrVarargImpl(startOffset, endOffset, arrayType, arrayElementType, arrayElements)
        val typeArguments = listOf(arrayElementType)

        return irCall(compilerContext.irBuiltIns.arrayOf, arrayType, typeArguments = typeArguments).apply {
            putValueArgument(0, arg0)
        }
    }

    fun IrBuilderWithScope.createPrimitiveArrayOfExpression(
        elementPrimitiveType: IrType,
        arrayElements: List<IrExpression>
    ): IrExpression {
        val arrayType = compilerContext.irBuiltIns.primitiveArrayForType.getValue(elementPrimitiveType).defaultType
        val arg0 = IrVarargImpl(startOffset, endOffset, arrayType, elementPrimitiveType, arrayElements)
        val typeArguments = listOf(elementPrimitiveType)

        return irCall(compilerContext.irBuiltIns.arrayOf, arrayType, typeArguments = typeArguments).apply {
            putValueArgument(0, arg0)
        }
    }

    fun IrBuilderWithScope.irBinOp(name: Name, lhs: IrExpression, rhs: IrExpression): IrExpression {
        val classFqName = (lhs.type as IrSimpleType).classOrNull!!.owner.fqNameWhenAvailable!!
        val symbol = compilerContext.referenceFunctions(CallableId(ClassId.topLevel(classFqName), name)).single()
        return irInvoke(lhs, symbol, rhs)
    }

    fun IrBuilderWithScope.irGetObject(irObject: IrClass) =
        IrGetObjectValueImpl(
            startOffset,
            endOffset,
            irObject.defaultType,
            irObject.symbol
        )

    fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration ->
            compilerContext.symbolTable.withReferenceScope(irDeclaration) {
                builder(irDeclaration)
            }
        }

    class BranchBuilder(
        val irWhen: IrWhen,
        context: IrGeneratorContext,
        scope: Scope,
        startOffset: Int,
        endOffset: Int
    ) : IrBuilderWithScope(context, scope, startOffset, endOffset) {
        operator fun IrBranch.unaryPlus() {
            irWhen.branches.add(this)
        }
    }

    fun IrBuilderWithScope.irWhen(typeHint: IrType? = null, block: BranchBuilder.() -> Unit): IrWhen {
        val whenExpr = IrWhenImpl(startOffset, endOffset, typeHint ?: compilerContext.irBuiltIns.unitType)
        val builder = BranchBuilder(whenExpr, context, scope, startOffset, endOffset)
        builder.block()
        return whenExpr
    }

    fun BranchBuilder.elseBranch(result: IrExpression): IrElseBranch =
        IrElseBranchImpl(
            IrConstImpl.boolean(result.startOffset, result.endOffset, compilerContext.irBuiltIns.booleanType, true),
            result
        )

    @Deprecated("", level = DeprecationLevel.ERROR)
    fun KotlinType.toIrType() = compilerContext.typeTranslator.translateType(this)

    // note: this method should be used only for properties from current module. Fields from other modules are private and inaccessible.
    val IrSerializableProperty.irField: IrField?
        get() = this.descriptor.backingField

    fun IrClass.searchForProperty(descriptor: PropertyDescriptor): IrProperty {
        // this API is used to reference both current module descriptors and external ones (because serializable class can be in any of them),
        // so we use descriptor api for current module because it is not possible to obtain FQname for e.g. local classes.
        return searchForDeclaration(descriptor) ?: if (descriptor.module == compilerContext.moduleDescriptor) {
            compilerContext.symbolTable.referenceProperty(descriptor).owner
        } else {
            @Suppress("DEPRECATION")
            compilerContext.referenceProperties(descriptor.fqNameSafe).single().owner
        }
    }



    /*
      Create a function that creates `get property value expressions` for given corresponded constructor's param
        (constructor_params) -> get_property_value_expression
     */
    fun IrBuilderWithScope.createPropertyByParamReplacer(
        irClass: IrClass,
        serialProperties: List<IrSerializableProperty>,
        instance: IrValueParameter,
        bindingContext: BindingContext?
    ): (ValueParameterDescriptor) -> IrExpression? {
        fun IrSerializableProperty.irGet(): IrExpression {
            val ownerType = instance.symbol.owner.type
            return getProperty(
                irGet(
                    type = ownerType,
                    variable = instance.symbol
                ), descriptor
            )
        }

        val serialPropertiesMap = serialProperties.associateBy { it.descriptor }

        val transientPropertiesSet =
            irClass.declarations.asSequence()
                .filterIsInstance<IrProperty>()
                .filter { it.backingField != null }
                .filter { !serialPropertiesMap.containsKey(it) }
                .toSet()

        return { vpd ->
            val propertyDescriptor = irClass.properties.find { it.name == vpd.name }
            if (propertyDescriptor != null) {
                val value = serialPropertiesMap[propertyDescriptor]
                value?.irGet() ?: run {
                    if (propertyDescriptor in transientPropertiesSet)
                        getProperty(
                            irGet(instance),
                            propertyDescriptor
                        )
                    else null
                }
            } else {
                null
            }
        }
    }

    fun IrBuilderWithScope.getProperty(receiver: IrExpression, property: IrProperty): IrExpression {
        return if (property.getter != null)
            irGet(property.getter!!.returnType, receiver, property.getter!!.symbol)
        else
            irGetField(receiver, property.backingField!!)
    }

    fun IrBuilderWithScope.setProperty(receiver: IrExpression, property: IrProperty, value: IrExpression): IrExpression {
        return if (property.setter != null)
            irSet(property.setter!!.returnType, receiver, property.setter!!.symbol, value)
        else
            irSetField(receiver, property.backingField!!, value)
    }

    /*
     The rest of the file is mainly copied from FunctionGenerator.
     However, I can't use it's directly because all generateSomething methods require KtProperty (psi element)
     Also, FunctionGenerator itself has DeclarationGenerator as ctor param, which is a part of psi2ir
     (it can be instantiated here, but I don't know how good is that idea)
     */

    fun IrBuilderWithScope.generateAnySuperConstructorCall(toBuilder: IrBlockBodyBuilder) {
        val anyConstructor = compilerContext.irBuiltIns.anyClass.owner.declarations.single { it is IrConstructor } as IrConstructor
        with(toBuilder) {
            +IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
                startOffset, endOffset,
                compilerContext.irBuiltIns.unitType,
                anyConstructor.symbol
            )
        }
    }

    fun IrBlockBodyBuilder.generateGoldenMaskCheck(
        seenVars: List<IrValueDeclaration>,
        properties: IrSerializableProperties,
        serialDescriptor: IrExpression
    ) {
        val fieldsMissedTest: IrExpression
        val throwErrorExpr: IrExpression

        val maskSlotCount = seenVars.size
        if (maskSlotCount == 1) {
            val goldenMask = properties.goldenMask


            throwErrorExpr = irInvoke(
                null,
                throwMissedFieldExceptionFunc!!,
                irGet(seenVars[0]),
                irInt(goldenMask),
                serialDescriptor,
                typeHint = compilerContext.irBuiltIns.unitType
            )

            fieldsMissedTest = irNotEquals(
                irInt(goldenMask),
                irBinOp(
                    OperatorNameConventions.AND,
                    irInt(goldenMask),
                    irGet(seenVars[0])
                )
            )
        } else {
            val goldenMaskList = properties.goldenMaskList

            var compositeExpression: IrExpression? = null
            for (i in goldenMaskList.indices) {
                val singleCheckExpr = irNotEquals(
                    irInt(goldenMaskList[i]),
                    irBinOp(
                        OperatorNameConventions.AND,
                        irInt(goldenMaskList[i]),
                        irGet(seenVars[i])
                    )
                )

                compositeExpression = if (compositeExpression == null) {
                    singleCheckExpr
                } else {
                    irBinOp(
                        OperatorNameConventions.OR,
                        compositeExpression,
                        singleCheckExpr
                    )
                }
            }

            fieldsMissedTest = compositeExpression!!

            throwErrorExpr = irBlock {
                +irInvoke(
                    null,
                    throwMissedFieldExceptionArrayFunc!!,
                    createPrimitiveArrayOfExpression(compilerContext.irBuiltIns.intType, goldenMaskList.indices.map { irGet(seenVars[it]) }),
                    createPrimitiveArrayOfExpression(compilerContext.irBuiltIns.intType, goldenMaskList.map { irInt(it) }),
                    serialDescriptor,
                    typeHint = compilerContext.irBuiltIns.unitType
                )
            }
        }

        +irIfThen(compilerContext.irBuiltIns.unitType, fieldsMissedTest, throwErrorExpr)
    }

    fun generateSimplePropertyWithBackingField(
        propertyDescriptor: PropertyDescriptor,
        propertyParent: IrClass,
        fieldName: Name = propertyDescriptor.name,
    ): IrProperty {
        val irProperty = propertyParent.searchForDeclaration(propertyDescriptor) ?: run {
            with(propertyDescriptor) {
                propertyParent.factory.createProperty(
                    propertyParent.startOffset, propertyParent.endOffset, SERIALIZABLE_PLUGIN_ORIGIN, IrPropertySymbolImpl(propertyDescriptor),
                    name, visibility, modality, isVar, isConst, isLateInit, isDelegated, isExternal
                ).also {
                    it.parent = propertyParent
                    propertyParent.addMember(it)
                }
            }
        }

        propertyParent.generatePropertyBackingFieldIfNeeded(propertyDescriptor, irProperty, fieldName)
        val fieldSymbol = irProperty.backingField!!.symbol
        irProperty.getter = propertyDescriptor.getter?.let {
            propertyParent.generatePropertyAccessor(propertyDescriptor, irProperty, it, fieldSymbol, isGetter = true)
        }?.apply { parent = propertyParent }
        irProperty.setter = propertyDescriptor.setter?.let {
            propertyParent.generatePropertyAccessor(propertyDescriptor, irProperty, it, fieldSymbol, isGetter = false)
        }?.apply { parent = propertyParent }
        return irProperty
    }

    fun IrType.kClassToJClassIfNeeded(): IrType = this

    fun kClassExprToJClassIfNeeded(startOffset: Int, endOffset: Int, irExpression: IrExpression): IrExpression = irExpression

    private fun IrClass.generatePropertyBackingFieldIfNeeded(
        propertyDescriptor: PropertyDescriptor,
        originProperty: IrProperty,
        name: Name,
    ) {
        if (originProperty.backingField != null) return

        val field = with(propertyDescriptor) {
            // TODO: type parameters
            @Suppress("DEPRECATION_ERROR") // should be called only with old FE
            originProperty.factory.createField(
                originProperty.startOffset, originProperty.endOffset, SERIALIZABLE_PLUGIN_ORIGIN, IrFieldSymbolImpl(propertyDescriptor), name, type.toIrType(),
                visibility, !isVar, isEffectivelyExternal(), dispatchReceiverParameter == null
            )
        }
        field.apply {
            parent = this@generatePropertyBackingFieldIfNeeded
            correspondingPropertySymbol = originProperty.symbol
        }

        originProperty.backingField = field
    }

    private fun IrClass.generatePropertyAccessor(
        propertyDescriptor: PropertyDescriptor,
        property: IrProperty,
        descriptor: PropertyAccessorDescriptor,
        fieldSymbol: IrFieldSymbol,
        isGetter: Boolean,
    ): IrSimpleFunction {
        val irAccessor: IrSimpleFunction = when (isGetter) {
            true -> searchForDeclaration<IrProperty>(propertyDescriptor)?.getter
            false -> searchForDeclaration<IrProperty>(propertyDescriptor)?.setter
        } ?: run {
            with(descriptor) {
                @Suppress("DEPRECATION_ERROR") // should never be called after FIR frontend
                property.factory.createFunction(
                    fieldSymbol.owner.startOffset, fieldSymbol.owner.endOffset, SERIALIZABLE_PLUGIN_ORIGIN, IrSimpleFunctionSymbolImpl(descriptor),
                    name, visibility, modality, returnType!!.toIrType(),
                    isInline, isEffectivelyExternal(), isTailrec, isSuspend, isOperator, isInfix, isExpect
                )
            }.also { f ->
                generateOverriddenFunctionSymbols(f, compilerContext.symbolTable)
                f.createParameterDeclarations(descriptor)
                @Suppress("DEPRECATION_ERROR") // should never be called after FIR frontend
                f.returnType = descriptor.returnType!!.toIrType()
                f.correspondingPropertySymbol = fieldSymbol.owner.correspondingPropertySymbol
            }
        }

        irAccessor.body = when (isGetter) {
            true -> {
                irAccessor.returnType = irAccessor.returnType.kClassToJClassIfNeeded()
                generateDefaultGetterBody(descriptor as PropertyGetterDescriptor, irAccessor)
            }
            false -> generateDefaultSetterBody(descriptor as PropertySetterDescriptor, irAccessor)
        }

        return irAccessor
    }

    private fun generateDefaultGetterBody(
        getter: PropertyGetterDescriptor,
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val property = getter.correspondingProperty
        val irProperty = irAccessor.correspondingPropertySymbol?.owner ?: error("Expected property for $getter")

        val startOffset = irAccessor.startOffset
        val endOffset = irAccessor.endOffset
        val irBody = irAccessor.factory.createBlockBody(startOffset, endOffset)

        val receiver = generateReceiverExpressionForFieldAccess(irAccessor.dispatchReceiverParameter!!.symbol, property)

        val propertyIrType = irAccessor.returnType
        irBody.statements.add(
            IrReturnImpl(
                startOffset, endOffset, compilerContext.irBuiltIns.nothingType,
                irAccessor.symbol,
                IrGetFieldImpl(
                    startOffset, endOffset,
                    irProperty.backingField?.symbol ?: error("Property expected to have backing field"),
                    propertyIrType,
                    receiver
                ).let {
                    if (propertyIrType.isKClass()) kClassExprToJClassIfNeeded(startOffset, endOffset, it) else it
                }
            )
        )
        return irBody
    }

    private fun generateDefaultSetterBody(
        setter: PropertySetterDescriptor,
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val property = setter.correspondingProperty
        val irProperty = irAccessor.correspondingPropertySymbol?.owner ?: error("Expected corresponding property for accessor $setter")
        val startOffset = irAccessor.startOffset
        val endOffset = irAccessor.endOffset
        val irBody = irAccessor.factory.createBlockBody(startOffset, endOffset)

        val receiver = generateReceiverExpressionForFieldAccess(irAccessor.dispatchReceiverParameter!!.symbol, property)

        val irValueParameter = irAccessor.valueParameters.single()
        irBody.statements.add(
            IrSetFieldImpl(
                startOffset, endOffset,
                irProperty.backingField?.symbol ?: error("Property $property expected to have backing field"),
                receiver,
                IrGetValueImpl(startOffset, endOffset, irValueParameter.type, irValueParameter.symbol),
                compilerContext.irBuiltIns.unitType
            )
        )
        return irBody
    }

    fun generateReceiverExpressionForFieldAccess( // todo: remove this
        ownerSymbol: IrValueSymbol,
        property: PropertyDescriptor
    ): IrExpression {
        val containingDeclaration = property.containingDeclaration
        return when (containingDeclaration) {
            is ClassDescriptor ->
                IrGetValueImpl(
                    ownerSymbol.owner.startOffset, ownerSymbol.owner.endOffset,
                    ownerSymbol
                )
            else -> throw AssertionError("Property must be in class")
        }
    }

    fun IrFunction.createParameterDeclarations(
        descriptor: FunctionDescriptor,
        overwriteValueParameters: Boolean = false,
        copyTypeParameters: Boolean = true
    ) {
        val function = this
        fun irValueParameter(descriptor: ParameterDescriptor): IrValueParameter = with(descriptor) {
            @Suppress("DEPRECATION_ERROR") // should never be called after FIR frontend
            factory.createValueParameter(
                function.startOffset, function.endOffset, SERIALIZABLE_PLUGIN_ORIGIN, IrValueParameterSymbolImpl(this),
                name, indexOrMinusOne, type.toIrType(), varargElementType?.toIrType(), isCrossinline, isNoinline,
                isHidden = false, isAssignable = false
            ).also {
                it.parent = function
            }
        }

        if (copyTypeParameters) {
            assert(typeParameters.isEmpty())
            copyTypeParamsFromDescriptor(descriptor)
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.let { irValueParameter(it) }
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.let { irValueParameter(it) }

        if (!overwriteValueParameters)
            assert(valueParameters.isEmpty())

        valueParameters = descriptor.valueParameters.map { irValueParameter(it) }
    }

    fun IrFunction.copyTypeParamsFromDescriptor(descriptor: FunctionDescriptor) {
        val newTypeParameters = descriptor.typeParameters.map {
            factory.createTypeParameter(
                startOffset, endOffset,
                SERIALIZABLE_PLUGIN_ORIGIN,
                IrTypeParameterSymbolImpl(it),
                it.name, it.index, it.isReified, it.variance
            ).also { typeParameter ->
                typeParameter.parent = this
            }
        }
        @Suppress("DEPRECATION_ERROR") // should never be called after FIR frontend
        newTypeParameters.forEach { typeParameter ->
            typeParameter.superTypes = typeParameter.descriptor.upperBounds.map { it.toIrType() }
        }

        typeParameters = newTypeParameters
    }

    fun createClassReference(classType: IrType, startOffset: Int, endOffset: Int): IrClassReference {
        return IrClassReferenceImpl(
            startOffset,
            endOffset,
            compilerContext.irBuiltIns.kClassClass.starProjectedType,
            classType.classifierOrFail,
            classType
        )
    }

    // It is impossible to use star projections right away, because Array<Int>::class differ from Array<String>::class :
    // detailed information about type arguments required for class references on jvm
    private val KotlinType.approximateJvmErasure: KotlinType
        get() = when (val classifier = constructor.declarationDescriptor) {
            is TypeParameterDescriptor -> {
                // Note that if the upper bound is Array, the argument type will be replaced with star here, which is probably incorrect.
                classifier.classBound.defaultType.replaceArgumentsWithStarProjections()
            }
            is ClassDescriptor -> if (KotlinBuiltIns.isArray(this)) {
                replace(arguments.map { TypeProjectionImpl(Variance.INVARIANT, it.type.approximateJvmErasure) })
            } else {
                replaceArgumentsWithStarProjections()
            }
            else -> error("Unsupported classifier: $this")
        }

    private val TypeParameterDescriptor.classBound: ClassDescriptor
        get() = when (val bound = representativeUpperBound.constructor.declarationDescriptor) {
            is ClassDescriptor -> bound
            is TypeParameterDescriptor -> bound.classBound
            else -> error("Unsupported classifier: $this")
        }


    fun IrBuilderWithScope.classReference(classSymbol: IrType): IrClassReference =
        createClassReference(classSymbol, startOffset, endOffset)

    private fun extractDefaultValuesFromConstructor(irClass: IrClass?): Map<ParameterDescriptor, IrExpression?> {
        if (irClass == null) return emptyMap()
        val original = irClass.constructors.singleOrNull { it.isPrimary }
        // default arguments of original constructor
        val defaultsMap: Map<ParameterDescriptor, IrExpression?> =
            original?.valueParameters?.associate { it.descriptor to it.defaultValue?.expression } ?: emptyMap()
        return defaultsMap + extractDefaultValuesFromConstructor(irClass.getSuperClassNotAny())
    }

    /*
    Creates an initializer adapter function that can replace IR expressions of getting constructor parameter value by some other expression.
    Also adapter may replace IR expression of getting `this` value by another expression.
     */
    fun createInitializerAdapter(
        irClass: IrClass,
        paramGetReplacer: (ValueParameterDescriptor) -> IrExpression?,
        thisGetReplacer: Pair<IrValueSymbol, () -> IrExpression>? = null
    ): (IrExpressionBody) -> IrExpression {
        val initializerTransformer = object : IrElementTransformerVoid() {
            // try to replace `get some value` expression
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val symbol = expression.symbol
                if (thisGetReplacer != null && thisGetReplacer.first == symbol) {
                    // replace `get this value` expression
                    return thisGetReplacer.second()
                }

                val descriptor = symbol.descriptor
                if (descriptor is ValueParameterDescriptor) {
                    // replace `get parameter value` expression
                    paramGetReplacer(descriptor)?.let { return it }
                }

                // otherwise leave expression as it is
                return super.visitGetValue(expression)
            }
        }
        val defaultsMap = extractDefaultValuesFromConstructor(irClass)
        return fun(initializer: IrExpressionBody): IrExpression {
            val rawExpression = initializer.expression
            val expression =
                if (rawExpression is IrGetValueImpl && rawExpression.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER) {
                    // this is a primary constructor property, use corresponding default of value parameter
                    defaultsMap.getValue(rawExpression.symbol.descriptor as ParameterDescriptor)!!
                } else {
                    rawExpression
                }
            return expression.deepCopyWithVariables().transform(initializerTransformer, null)
        }
    }

    private fun getEnumMembersNames(enumClass: ClassDescriptor): Sequence<String> {
        assert(enumClass.kind == ClassKind.ENUM_CLASS)
        return enumClass.unsubstitutedMemberScope.getContributedDescriptors().asSequence()
            .filterIsInstance<ClassDescriptor>()
            .filter { it.kind == ClassKind.ENUM_ENTRY }
            .map { it.name.toString() }
    }

    fun IrBuilderWithScope.copyAnnotationsFrom(annotations: List<IrConstructorCall>): List<IrExpression> =
        annotations.mapNotNull { annotationCall ->
            val annotationClass = annotationCall.symbol.owner.parentAsClass
            if (!annotationClass.descriptor.isSerialInfoAnnotation) return@mapNotNull null

            if (compilerContext.platform.isJvm()) {
                val implClass = compilerContext.serialInfoImplJvmIrGenerator.getImplClass(annotationClass)
                val ctor = implClass.constructors.singleOrNull { it.valueParameters.size == annotationCall.valueArgumentsCount }
                    ?: error("No constructor args found for SerialInfo annotation Impl class: ${implClass.render()}")
                irCall(ctor).apply {
                    for (i in 0 until annotationCall.valueArgumentsCount) {
                        val argument = annotationCall.getValueArgument(i)
                            ?: annotationClass.primaryConstructor!!.valueParameters[i].defaultValue?.expression
                        putValueArgument(i, argument!!.deepCopyWithVariables())
                    }
                }
            } else {
                annotationCall.deepCopyWithVariables()
            }
        }

    // Does not use sti and therefore does not perform encoder calls optimization
    fun IrBuilderWithScope.serializerTower(
        generator: SerializerIrGenerator,
        dispatchReceiverParameter: IrValueParameter,
        property: IrSerializableProperty
    ): IrExpression? {
        val nullableSerClass = compilerContext.referenceProperties(SerialEntityNames.wrapIntoNullableCallableId).single()
        val serializer =
            property.serializableWith?.classOrNull
                ?: if (!property.type.isTypeParameter()) generator.findTypeSerializerOrContext(
                    compilerContext,
                    property.type
                ) else null
        return serializerInstance(
            generator,
            dispatchReceiverParameter,
            serializer,
            compilerContext,
            property.type,
            genericIndex = property.genericIndex
        )
            ?.let { expr -> wrapWithNullableSerializerIfNeeded(property.type, expr, nullableSerClass) }
    }

    private fun IrBuilderWithScope.wrapWithNullableSerializerIfNeeded(
        type: IrType,
        expression: IrExpression,
        nullableProp: IrPropertySymbol
    ): IrExpression = if (type.isMarkedNullable()) {
        val resultType = type.makeNotNull()
        val typeArguments = listOf(resultType)
        val callee = nullableProp.owner.getter!!

        val returnType = callee.returnType.substitute(callee.typeParameters, typeArguments)

        irInvoke(
            callee = callee.symbol,
            typeArguments = typeArguments,
            valueArguments = emptyList(),
            returnTypeHint = returnType
        ).apply { extensionReceiver = expression }
    } else {
        expression
    }

//    private fun IrBuilderWithScope.wrapWithNullableSerializerIfNeeded(
//        type: KotlinType,
//        expression: IrExpression,
//        nullableProp: IrPropertySymbol
//    ): IrExpression = wrapWithNullableSerializerIfNeeded(type.toIrType(), expression, nullableProp)


    fun wrapIrTypeIntoKSerializerIrType(
        type: IrType,
        variance: Variance = Variance.INVARIANT
    ): IrType {
        val kSerClass = compilerContext.referenceClass(ClassId(SerializationPackages.packageFqName, SerialEntityNames.KSERIALIZER_NAME))
            ?: error("Couldn't find class ${SerialEntityNames.KSERIALIZER_NAME}")
        return IrSimpleTypeImpl(
            kSerClass, hasQuestionMark = false, arguments = listOf(
                makeTypeProjection(type, variance)
            ), annotations = emptyList()
        )
    }

    fun IrBuilderWithScope.serializerInstance(
        enclosingGenerator: SerializerIrGenerator,
        dispatchReceiverParameter: IrValueParameter,
        serializerClassOriginal: IrClassSymbol?,
        pluginContext: SerializationPluginContext,
        kType: IrType,
        genericIndex: Int? = null
    ): IrExpression? = serializerInstance(
        enclosingGenerator,
        serializerClassOriginal,
        pluginContext,
        kType,
        genericIndex
    ) { it, _ ->
        val (_, ir) = enclosingGenerator.localSerializersFieldsDescriptors[it]
        irGetField(irGet(dispatchReceiverParameter), ir.backingField!!)
    }

    fun IrBuilderWithScope.serializerInstance(
        enclosingGenerator: AbstractSerialGenerator,
        serializerClassOriginal: ClassDescriptor?,
        module: ModuleDescriptor,
        kType: IrType,
        genericIndex: Int? = null,
        genericGetter: ((Int) -> IrExpression)? = null
    ): IrExpression? {
        return serializerInstance(
            enclosingGenerator,
            serializerClassOriginal?.let { compilerContext.symbolTable.referenceClass(it) },
            compilerContext,
            kType,
            genericIndex,
            if (genericGetter != null) { i, _ -> genericGetter(i) } else null
        )
    }

    fun IrBuilderWithScope.serializerInstance(
        enclosingGenerator: AbstractSerialGenerator,
        serializerClassOriginal: IrClassSymbol?,
        pluginContext: SerializationPluginContext,
        kType: IrType,
        genericIndex: Int? = null,
        genericGetter: ((Int, IrType) -> IrExpression)? = null
    ): IrExpression? {
        val nullableSerClass = compilerContext.referenceProperties(SerialEntityNames.wrapIntoNullableCallableId).single()
        if (serializerClassOriginal == null) {
            if (genericIndex == null) return null
            return genericGetter?.invoke(genericIndex, kType)
        }
        if (serializerClassOriginal.owner.kind == ClassKind.OBJECT) {
            return irGetObject(serializerClassOriginal)
        }
        fun instantiate(serializer: IrClassSymbol?, type: IrType): IrExpression? {
            val expr = serializerInstance(
                enclosingGenerator,
                serializer,
                pluginContext,
                type,
                type.genericIndex,
                genericGetter
            ) ?: return null
            return wrapWithNullableSerializerIfNeeded(type, expr, nullableSerClass)
        }

        var serializerClass = serializerClassOriginal
        var args: List<IrExpression>
        var typeArgs: List<IrType>
        val thisIrType = (kType as? IrSimpleType) ?: error("Don't know how to work with type ${kType::class}")
        var needToCopyAnnotations = false

        when (serializerClassOriginal.owner.classId) {
            polymorphicSerializerId -> {
                needToCopyAnnotations = true
                args = listOf(classReference(kType))
                typeArgs = listOf(thisIrType)
            }
            contextSerializerId -> {
                args = listOf(classReference(kType))
                typeArgs = listOf(thisIrType)

                val hasNewCtxSerCtor = compilerContext.referenceConstructors(contextSerializerId).any { it.owner.valueParameters.size == 3 }

                if (hasNewCtxSerCtor) {
                    // new signature of context serializer
                    args = args + mutableListOf<IrExpression>().apply {
                        val fallbackDefaultSerializer = findTypeSerializer(pluginContext, kType)
                        add(instantiate(fallbackDefaultSerializer, kType) ?: irNull())
                        add(
                            createArrayOfExpression(
                                wrapIrTypeIntoKSerializerIrType(
                                    thisIrType,
                                    variance = Variance.OUT_VARIANCE
                                ),
                                thisIrType.arguments.map {
                                    val argSer = enclosingGenerator.findTypeSerializerOrContext(
                                        compilerContext,
                                        it.typeOrNull!!, //todo: handle star projections here
                                        sourceElement = serializerClassOriginal.owner.sourceElement()?.psi
                                    )
                                    instantiate(argSer, it.typeOrNull!!)!!
                                })
                        )
                    }
                }
            }
            objectSerializerId -> {
                needToCopyAnnotations = true
                args = listOf(irString(kType.serialName()), irGetObject(kType.classOrNull!!))
                typeArgs = listOf(thisIrType)
            }
            sealedSerializerId -> {
                needToCopyAnnotations = true
                args = mutableListOf<IrExpression>().apply {
                    add(irString(kType.serialName()))
                    add(classReference(kType))
                    val (subclasses, subSerializers) = enclosingGenerator.allSealedSerializableSubclassesFor(
                        kType.classOrNull!!.owner,
                        pluginContext
                    )
                    val projectedOutCurrentKClass =
                        compilerContext.irBuiltIns.kClassClass.typeWithArguments(
                            listOf(makeTypeProjection(thisIrType, Variance.OUT_VARIANCE))
                        )
                    add(
                        createArrayOfExpression(
                            projectedOutCurrentKClass,
                            subclasses.map { classReference(it) }
                        )
                    )
                    add(
                        createArrayOfExpression(
                            wrapIrTypeIntoKSerializerIrType(thisIrType, variance = Variance.OUT_VARIANCE),
                            subSerializers.mapIndexed { i, serializer ->
                                val type = subclasses[i]
                                val expr = serializerInstance(
                                    enclosingGenerator,
                                    serializer,
                                    pluginContext,
                                    type,
                                    type.genericIndex
                                ) { _, genericType ->
                                    serializerInstance(
                                        enclosingGenerator,
                                        pluginContext.referenceClass(polymorphicSerializerId),
                                        pluginContext,
                                        (genericType.classifierOrNull as IrTypeParameterSymbol).owner.representativeUpperBound
                                    )!!
                                }!!
                                wrapWithNullableSerializerIfNeeded(type, expr, nullableSerClass)
                            }
                        )
                    )
                }
                typeArgs = listOf(thisIrType)
            }
            enumSerializerId -> {
                serializerClass = pluginContext.referenceClass(enumSerializerId)
                val enumDescriptor = kType.classOrNull!!
                typeArgs = listOf(thisIrType)
                // instantiate serializer only inside enum Companion
                if (enclosingGenerator !is SerializableCompanionIrGenerator) {
                    // otherwise call Companion.serializer()
                    callSerializerFromCompanion(thisIrType, typeArgs, emptyList())?.let { return it }
                }

                val enumArgs = mutableListOf(
                    irString(thisIrType.serialName()),
                    irCall(enumDescriptor.owner.findEnumValuesMethod()),
                )

                val enumSerializerFactoryFunc = enumSerializerFactoryFunc
                val markedEnumSerializerFactoryFunc = markedEnumSerializerFactoryFunc
                if (enumSerializerFactoryFunc != null && markedEnumSerializerFactoryFunc != null) {
                    // runtime contains enum serializer factory functions
                    val factoryFunc: IrSimpleFunctionSymbol = if (enumDescriptor.owner.isEnumWithSerialInfoAnnotation()) {
                        // need to store SerialInfo annotation in descriptor
                        val enumEntries = enumDescriptor.owner.enumEntries()
                        val entriesNames = enumEntries.map { it.annotations.serialNameValue?.let { n -> irString(n) } ?: irNull() }
                        val entriesAnnotations = enumEntries.map {
                            val annotationConstructors = it.annotations.map { a ->
//                                compilerContext.typeTranslator.constantValueGenerator.generateAnnotationConstructorCall(a)
                                a.deepCopyWithVariables()
                            }
                            val annotationsConstructors = copyAnnotationsFrom(annotationConstructors)
                            if (annotationsConstructors.isEmpty()) {
                                irNull()
                            } else {
                                createArrayOfExpression(compilerContext.irBuiltIns.annotationType, annotationsConstructors)
                            }
                        }
                        val annotationArrayType =
                            compilerContext.irBuiltIns.arrayClass.typeWith(compilerContext.irBuiltIns.annotationType.makeNullable())

                        enumArgs += createArrayOfExpression(compilerContext.irBuiltIns.stringType.makeNullable(), entriesNames)
                        enumArgs += createArrayOfExpression(annotationArrayType, entriesAnnotations)

                        markedEnumSerializerFactoryFunc
                    } else {
                        enumSerializerFactoryFunc
                    }

                    val factoryReturnType = factoryFunc.owner.returnType.substitute(factoryFunc.owner.typeParameters, typeArgs)
                    return irInvoke(null, factoryFunc, typeArgs, enumArgs, factoryReturnType)
                } else {
                    // support legacy serializer instantiation by constructor for old runtimes
                    args = enumArgs
                }
            }
            else -> {
                args = kType.arguments.map {
                    val argSer = enclosingGenerator.findTypeSerializerOrContext(
                        pluginContext,
                        it.typeOrNull!!, // todo: stars
                        sourceElement = serializerClassOriginal.owner.source.getPsi()
                    )
                    instantiate(argSer, it.typeOrNull!!) ?: return null
                }
                typeArgs = kType.arguments.map { it.typeOrNull!! }
            }

        }
        if (serializerClassOriginal.owner.classId == referenceArraySerializerId) {
            args = listOf(wrapperClassReference(kType.arguments.single().typeOrNull!!)) + args
            typeArgs = listOf(typeArgs[0].makeNotNull()) + typeArgs
        }

        // If KType is interface, .classSerializer always yields PolymorphicSerializer, which may be unavailable for interfaces from other modules
        if (!kType.isInterface() && serializerClassOriginal == kType.classOrNull!!.owner.classSerializer(pluginContext) && enclosingGenerator !is SerializableCompanionIrGenerator) {
            // This is default type serializer, we can shortcut through Companion.serializer()
            // BUT not during generation of this method itself
            callSerializerFromCompanion(thisIrType, typeArgs, args)?.let { return it }
        }


        val serializable = serializerClass?.owner?.let { getSerializableClassDescriptorBySerializer(it) }
        requireNotNull(serializerClass)
        val ctor = if (serializable?.typeParameters?.isNotEmpty() == true) {
            requireNotNull(
                findSerializerConstructorForTypeArgumentsSerializers(serializerClass.owner)
            ) { "Generated serializer does not have constructor with required number of arguments" }
        } else {
            val constructors = serializerClass.constructors
            // search for new signature of polymorphic/sealed/contextual serializer
            if (!needToCopyAnnotations) {
                constructors.single { it.owner.isPrimary }
            } else {
                constructors.find { it.owner.lastArgumentIsAnnotationArray() } ?: run {
                    // not found - we are using old serialization runtime without this feature
                    needToCopyAnnotations = false
                    constructors.single { it.owner.isPrimary }
                }
            }
        }
        // Return type should be correctly substituted
        assert(ctor.isBound)
        val ctorDecl = ctor.owner
        if (needToCopyAnnotations) {
            val classAnnotations = copyAnnotationsFrom(thisIrType.getClass()?.let { collectSerialInfoAnnotations(it) }.orEmpty())
            args = args + createArrayOfExpression(compilerContext.irBuiltIns.annotationType, classAnnotations)
        }

        val typeParameters = ctorDecl.parentAsClass.typeParameters
        val substitutedReturnType = ctorDecl.returnType.substitute(typeParameters, typeArgs)
        return irInvoke(
            null,
            ctor,
            // User may declare serializer with fixed type arguments, e.g. class SomeSerializer : KSerializer<ClosedRange<Float>>
            typeArguments = typeArgs.takeIf { it.size == ctorDecl.typeParameters.size }.orEmpty(),
            valueArguments = args.takeIf { it.size == ctorDecl.valueParameters.size }.orEmpty(),
            returnTypeHint = substitutedReturnType
        )
    }

    fun collectSerialInfoAnnotations(irClass: IrClass): List<IrConstructorCall> {
        if (!(irClass.isInterface || irClass.descriptor.hasSerializableOrMetaAnnotation)) return emptyList()
        val annotationByFq: MutableMap<FqName, IrConstructorCall> =
            irClass.annotations.associateBy { it.symbol.owner.parentAsClass.descriptor.fqNameSafe }.toMutableMap()
        for (clazz in irClass.getAllSuperclasses()) {
            val annotations = clazz.annotations
                .mapNotNull {
                    val descriptor = it.symbol.owner.parentAsClass.descriptor
                    if (descriptor.isInheritableSerialInfoAnnotation) descriptor.fqNameSafe to it else null
                }
            annotations.forEach { (fqname, call) ->
                if (fqname !in annotationByFq) {
                    annotationByFq[fqname] = call
                } else {
                    // SerializationPluginDeclarationChecker already reported inconsistency
                }
            }
        }
        return annotationByFq.values.toList()
    }

    fun IrBuilderWithScope.callSerializerFromCompanion(
        thisIrType: IrType,
        typeArgs: List<IrType>,
        args: List<IrExpression>
    ): IrExpression? {
        val baseClass = thisIrType.getClass() ?: return null
        val companionClass = baseClass.companionObject() ?: return null
        val serializerProviderFunction = companionClass.declarations.singleOrNull {
            it is IrFunction && it.name == SerialEntityNames.SERIALIZER_PROVIDER_NAME && it.valueParameters.size == baseClass.typeParameters.size
        } ?: return null

        val adjustedArgs: List<IrExpression> =
            // if typeArgs.size == args.size then the serializer is custom - we need to use the actual serializers from the arguments
            if ((typeArgs.size != args.size) && (baseClass.descriptor.isSealed() || baseClass.descriptor.modality == Modality.ABSTRACT)) {
                val serializer = findStandardKotlinTypeSerializer(compilerContext, context.irBuiltIns.unitType)!!
                // workaround for sealed and abstract classes - the `serializer` function expects non-null serializers, but does not use them, so serializers of any type can be passed
                List(baseClass.typeParameters.size) { irGetObject(serializer) }
            } else {
                args
            }

        with(serializerProviderFunction as IrFunction) {
            // Note that [typeArgs] may be unused if we short-cut to e.g. SealedClassSerializer
            return irInvoke(
                irGetObject(companionClass),
                symbol,
                typeArgs.takeIf { it.size == typeParameters.size }.orEmpty(),
                adjustedArgs.takeIf { it.size == valueParameters.size }.orEmpty()
            )
        }
    }

    private fun IrConstructor.lastArgumentIsAnnotationArray(): Boolean {
        val lastArgType = valueParameters.lastOrNull()?.type
        if (lastArgType == null || !lastArgType.isArray()) return false
        return ((lastArgType as? IrSimpleType)?.arguments?.firstOrNull()?.typeOrNull?.classFqName?.toString() == "kotlin.Annotation")
    }

    private fun IrBuilderWithScope.wrapperClassReference(classType: IrType): IrClassReference {
        if (compilerContext.platform.isJvm()) {
            // "Byte::class" -> "java.lang.Byte::class"
//          TODO: get rid of descriptor
            val wrapperFqName = KotlinBuiltIns.getPrimitiveType(classType.classOrNull!!.descriptor)?.let(JvmPrimitiveType::get)?.wrapperFqName
            if (wrapperFqName != null) {
                val wrapperClass = compilerContext.referenceClass(ClassId.topLevel(wrapperFqName))
                    ?: error("Primitive wrapper class for $classType not found: $wrapperFqName")
                return classReference(wrapperClass.defaultType)
            }
        }
        return classReference(classType)
    }

    fun findSerializerConstructorForTypeArgumentsSerializers(serializer: IrClass): IrConstructorSymbol? {

        val typeParamsCount = ((serializer.superTypes.find { isKSerializer(it) } as IrSimpleType).arguments.first().typeOrNull!! as IrSimpleType).arguments.size
        if (typeParamsCount == 0) return null //don't need it

        return serializer.constructors.singleOrNull {
            it.valueParameters.let { vps -> vps.size == typeParamsCount && vps.all { vp -> isKSerializer(vp.type) } }
        }?.symbol
    }

    private fun IrConstructor.isSerializationCtor(): Boolean {
        val serialMarker =
            compilerContext.referenceClass(ClassId.topLevel(SerializationPackages.internalPackageFqName.child(SerialEntityNames.SERIAL_CTOR_MARKER_NAME)))

        return valueParameters.lastOrNull()?.run {
            name == SerialEntityNames.dummyParamName && type.classifierOrNull == serialMarker
        } == true
    }

    fun serializableSyntheticConstructor(forClass: IrClass): IrConstructorSymbol {
        return forClass.declarations.filterIsInstance<IrConstructor>().single { it.isSerializationCtor() }.symbol
    }

    fun IrClass.getSuperClassOrAny(): IrClass = getSuperClassNotAny() ?: compilerContext.irBuiltIns.anyClass.owner

    fun IrClass.getSuperClassNotAny(): IrClass? {
        val superClasses = superTypes.mapNotNull { it.classOrNull }.map { it.owner }

        return superClasses.singleOrNull { it.kind == ClassKind.CLASS }
    }

    fun IrClass.findWriteSelfMethod(): IrSimpleFunction? =
        declarations.filter { it is IrSimpleFunction && it.name == SerialEntityNames.WRITE_SELF_NAME && !it.isFakeOverride }
            .takeUnless(Collection<*>::isEmpty)?.single() as IrSimpleFunction?

    fun IrBlockBodyBuilder.serializeAllProperties(
        generator: AbstractSerialGenerator,
        serializableIrClass: IrClass,
        serializableProperties: List<IrSerializableProperty>,
        objectToSerialize: IrValueDeclaration,
        localOutput: IrValueDeclaration,
        localSerialDesc: IrValueDeclaration,
        kOutputClass: IrClassSymbol,
        ignoreIndexTo: Int,
        initializerAdapter: (IrExpressionBody) -> IrExpression,
        genericGetter: ((Int, IrType) -> IrExpression)?
    ) {

        fun IrSerializableProperty.irGet(): IrExpression {
            val ownerType = objectToSerialize.symbol.owner.type
            return getProperty(
                irGet(
                    type = ownerType,
                    variable = objectToSerialize.symbol
                ), descriptor
            )
        }

        for ((index, property) in serializableProperties.withIndex()) {
            if (index < ignoreIndexTo) continue
            // output.writeXxxElementValue(classDesc, index, value)
            val elementCall = formEncodeDecodePropertyCall(
                generator,
                irGet(localOutput),
                property, { innerSerial, sti ->
                    val f =
                        kOutputClass.functionByName("${CallingConventions.encode}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}")
                    f to listOf(
                        irGet(localSerialDesc),
                        irInt(index),
                        innerSerial,
                        property.irGet()
                    )
                }, {
                    val f =
                        kOutputClass.functionByName("${CallingConventions.encode}${it.elementMethodPrefix}${CallingConventions.elementPostfix}")
                    val args: MutableList<IrExpression> = mutableListOf(irGet(localSerialDesc), irInt(index))
                    if (it.elementMethodPrefix != "Unit") args.add(property.irGet())
                    f to args
                },
                genericGetter
            )

            // check for call to .shouldEncodeElementDefault
            val encodeDefaults = property.descriptor.getEncodeDefaultAnnotationValue()
            val field = property.irField // Nullable when property from another module; can't compare it with default value on JS or Native
            if (!property.optional || encodeDefaults == true || field == null) {
                // emit call right away
                +elementCall
            } else {
                val partB = irNotEquals(property.irGet(), initializerAdapter(field.initializer!!))

                val condition = if (encodeDefaults == false) {
                    // drop default without call to .shouldEncodeElementDefault
                    partB
                } else {
                    // emit check:
                    // if (if (output.shouldEncodeElementDefault(this.descriptor, i)) true else {obj.prop != DEFAULT_VALUE} ) {
                    //    output.encodeIntElement(this.descriptor, i, obj.prop)// block {obj.prop != DEFAULT_VALUE} may contain several statements
                    val shouldEncodeFunc = kOutputClass.functionByName(CallingConventions.shouldEncodeDefault)
                    val partA = irInvoke(irGet(localOutput), shouldEncodeFunc, irGet(localSerialDesc), irInt(index))
                    // Ir infrastructure does not have dedicated symbol for ||, so
                    //  `a || b == if (a) true else b`, see org.jetbrains.kotlin.ir.builders.PrimitivesKt.oror
                    irIfThenElse(compilerContext.irBuiltIns.booleanType, partA, irTrue(), partB)
                }
                +irIfThen(condition, elementCall)
            }
        }
    }

    /**
     * True — ALWAYS
     * False — NEVER
     * null — not specified
     */
    fun IrProperty.getEncodeDefaultAnnotationValue(): Boolean? {
        val call = annotations.findAnnotation(SerializationAnnotations.encodeDefaultFqName) ?: return null
        val arg = call.getValueArgument(0) ?: return true // ALWAYS by default
        val argValue = (arg as? IrGetEnumValue
            ?: error("Argument of enum constructor expected to implement IrGetEnumValue, got $arg")).symbol.owner.name.toString()
        return when (argValue) {
            "ALWAYS" -> true
            "NEVER" -> false
            else -> error("Unknown EncodeDefaultMode enum value: $argValue")
        }
    }


    fun IrBlockBodyBuilder.formEncodeDecodePropertyCall(
        enclosingGenerator: AbstractSerialGenerator,
        encoder: IrExpression,
        property: IrSerializableProperty,
        whenHaveSerializer: (serializer: IrExpression, sti: IrSerialTypeInfo) -> FunctionWithArgs,
        whenDoNot: (sti: IrSerialTypeInfo) -> FunctionWithArgs,
        genericGetter: ((Int, IrType) -> IrExpression)? = null,
        returnTypeHint: IrType? = null
    ): IrExpression {
        val sti = enclosingGenerator.getIrSerialTypeInfo(property, compilerContext)
        val innerSerial = serializerInstance(
            enclosingGenerator,
            sti.serializer,
            compilerContext,
            property.type,
            property.genericIndex,
            genericGetter
        )
        val (functionToCall, args: List<IrExpression>) = if (innerSerial != null) whenHaveSerializer(innerSerial, sti) else whenDoNot(sti)
        val typeArgs = if (functionToCall.descriptor.typeParameters.isNotEmpty()) listOf(property.type) else listOf()
        return irInvoke(encoder, functionToCall, typeArguments = typeArgs, valueArguments = args, returnTypeHint = returnTypeHint)
    }

}
