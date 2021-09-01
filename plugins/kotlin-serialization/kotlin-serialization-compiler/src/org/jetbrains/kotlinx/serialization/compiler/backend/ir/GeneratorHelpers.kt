/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.*
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
        get() = compilerContext.referenceFunctions(SerialEntityNames.SINGLE_MASK_FIELD_MISSING_FUNC_FQ).singleOrNull()

    private val throwMissedFieldExceptionArrayFunc
        get() = compilerContext.referenceFunctions(SerialEntityNames.ARRAY_MASK_FIELD_MISSING_FUNC_FQ).singleOrNull()

    private inline fun <reified T : IrDeclaration> IrClass.searchForDeclaration(descriptor: DeclarationDescriptor): T? {
        return declarations.singleOrNull { it.descriptor == descriptor } as? T
    }

    fun useFieldMissingOptimization(): Boolean {
        return throwMissedFieldExceptionFunc != null && throwMissedFieldExceptionArrayFunc != null
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

        val f0Type = module.findClassAcrossModuleDependencies(ClassId.topLevel(FUNCTION0_FQ))!!.defaultType
        val f0ParamSymbol = compilerContext.symbolTable.referenceTypeParameter(f0Type.constructor.parameters[0])
        val f0IrType = f0Type.toIrType().substitute(mapOf(f0ParamSymbol to type))

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
        val lazySafeModeClassDescriptor = compilerContext.referenceClass(LAZY_MODE_FQ)!!.descriptor
        val lazyFunctionSymbol = compilerContext.referenceFunctions(LAZY_FUNC_FQ).single {
            it.descriptor.valueParameters.size == 2 && it.descriptor.valueParameters[0].type == lazySafeModeClassDescriptor.defaultType
        }
        val publicationEntryDescriptor = lazySafeModeClassDescriptor.enumEntries().single { it.name == LAZY_PUBLICATION_MODE_NAME }

        val lazyIrClass = compilerContext.referenceClass(LAZY_FQ)!!.owner
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
                    publicationEntryDescriptor.classValueType!!.toIrType(),
                    compilerContext.symbolTable.referenceEnumEntry(publicationEntryDescriptor)
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
        val lazyIrClass = compilerContext.referenceClass(LAZY_FQ)!!.owner
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

    fun ClassDescriptor.referenceFunctionSymbol(
        functionName: String,
        predicate: (IrSimpleFunction) -> Boolean = { true }
    ): IrFunctionSymbol {
        val irClass = compilerContext.referenceClass(fqNameSafe)?.owner ?: error("Couldn't load class $this")
        val simpleFunctions = irClass.declarations.filterIsInstance<IrSimpleFunction>()

        return simpleFunctions.filter { it.name.asString() == functionName }.single { predicate(it) }.symbol
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
        val symbol = compilerContext.referenceFunctions(classFqName.child(name)).single()
        return irInvoke(lhs, symbol, rhs)
    }

    fun IrBuilderWithScope.irGetObject(classDescriptor: ClassDescriptor) =
        IrGetObjectValueImpl(
            startOffset,
            endOffset,
            classDescriptor.defaultType.toIrType(),
            compilerContext.symbolTable.referenceClass(classDescriptor)
        )

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

    fun IrBuilderWithScope.irEmptyVararg(forValueParameter: ValueParameterDescriptor) =
        IrVarargImpl(
            startOffset,
            endOffset,
            forValueParameter.type.toIrType(),
            forValueParameter.varargElementType!!.toIrType()
        )

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

    fun KotlinType.toIrType() = compilerContext.typeTranslator.translateType(this)

    // note: this method should be used only for properties from current module. Fields from other modules are private and inaccessible.
    val SerializableProperty.irField: IrField? get() = compilerContext.symbolTable.referenceField(this.descriptor).let {  if (it.isBound) it.owner else null }

    fun IrClass.searchForProperty(descriptor: PropertyDescriptor): IrProperty {
        // this API is used to reference both current module descriptors and external ones (because serializable class can be in any of them),
        // so we use descriptor api for current module because it is not possible to obtain FQname for e.g. local classes.
        return searchForDeclaration(descriptor) ?: if (descriptor.module == compilerContext.moduleDescriptor) {
            compilerContext.symbolTable.referenceProperty(descriptor).owner
        } else {
            compilerContext.referenceProperties(descriptor.fqNameSafe).single().owner
        }
    }

    fun SerializableProperty.getIrPropertyFrom(thisClass: IrClass): IrProperty {
        return thisClass.searchForProperty(descriptor)
    }


    /*
      Create a function that creates `get property value expressions` for given corresponded constructor's param
        (constructor_params) -> get_property_value_expression
     */
    fun IrBuilderWithScope.createPropertyByParamReplacer(
        irClass: IrClass,
        serialProperties: List<SerializableProperty>,
        instance: IrValueParameter,
        bindingContext: BindingContext
    ): (ValueParameterDescriptor) -> IrExpression? {
        fun SerializableProperty.irGet(): IrExpression {
            val ownerType = instance.symbol.owner.type
            return getProperty(
                irGet(
                    type = ownerType,
                    variable = instance.symbol
                ), getIrPropertyFrom(irClass)
            )
        }

        val serialPropertiesMap = serialProperties.associateBy { it.descriptor }

        val transientPropertiesMap =
            irClass.declarations.asSequence()
                .filterIsInstance<IrProperty>()
                .filter { it.backingField != null }.filter { !serialPropertiesMap.containsKey(it.descriptor) }
                .associateBy { it.symbol.descriptor }

        return {
            val propertyDescriptor = bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, it]
            if (propertyDescriptor != null) {
                val value = serialPropertiesMap[propertyDescriptor]
                value?.irGet() ?: transientPropertiesMap[propertyDescriptor]?.let { prop ->
                    getProperty(
                        irGet(instance),
                        prop
                    )
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
        properties: SerializableProperties,
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
        val irProperty = propertyParent.searchForDeclaration<IrProperty>(propertyDescriptor) ?: run {
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

    private fun IrClass.generatePropertyBackingFieldIfNeeded(
        propertyDescriptor: PropertyDescriptor,
        originProperty: IrProperty,
        name: Name,
    ) {
        if (originProperty.backingField != null) return

        val field = with(propertyDescriptor) {
            // TODO: type parameters
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
                property.factory.createFunction(
                    fieldSymbol.owner.startOffset, fieldSymbol.owner.endOffset, SERIALIZABLE_PLUGIN_ORIGIN, IrSimpleFunctionSymbolImpl(descriptor),
                    name, visibility, modality, returnType!!.toIrType(),
                    isInline, isEffectivelyExternal(), isTailrec, isSuspend, isOperator, isInfix, isExpect
                )
            }.also { f ->
                generateOverriddenFunctionSymbols(f, compilerContext.symbolTable)
                f.createParameterDeclarations(descriptor)
                f.returnType = descriptor.returnType!!.toIrType()
                f.correspondingPropertySymbol = fieldSymbol.owner.correspondingPropertySymbol
            }
        }

        irAccessor.body = when (isGetter) {
            true -> generateDefaultGetterBody(descriptor as PropertyGetterDescriptor, irAccessor)
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

        irBody.statements.add(
            IrReturnImpl(
                startOffset, endOffset, compilerContext.irBuiltIns.nothingType,
                irAccessor.symbol,
                IrGetFieldImpl(
                    startOffset, endOffset,
                    irProperty.backingField?.symbol ?: error("Property expected to have backing field"),
                    property.type.toIrType(),
                    receiver
                )
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

    fun generateReceiverExpressionForFieldAccess(
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

        newTypeParameters.forEach { typeParameter ->
            typeParameter.superTypes = typeParameter.descriptor.upperBounds.map { it.toIrType() }
        }

        typeParameters = newTypeParameters
    }

    fun createClassReference(classType: KotlinType, startOffset: Int, endOffset: Int): IrClassReference {
        val clazz = classType.toClassDescriptor!!
        val classSymbol = compilerContext.referenceClass(clazz.fqNameSafe) ?: error("Couldn't load class $clazz")
        return IrClassReferenceImpl(
            startOffset,
            endOffset,
            compilerContext.irBuiltIns.kClassClass.starProjectedType,
            classSymbol,
            classType.approximateJvmErasure.toIrType()
        )
    }

    fun createClassReference(irClass: IrClass, startOffset: Int, endOffset: Int): IrClassReference {
        val classType = irClass.descriptor.toSimpleType(false)
        val classSymbol = irClass.symbol
        return IrClassReferenceImpl(
            startOffset,
            endOffset,
            compilerContext.irBuiltIns.kClassClass.starProjectedType,
            classSymbol,
            classType.approximateJvmErasure.toIrType()
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

    fun IrBuilderWithScope.classReference(classType: KotlinType): IrClassReference = createClassReference(classType, startOffset, endOffset)

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

    fun findEnumValuesMethod(enumClass: ClassDescriptor): IrFunction {
        assert(enumClass.kind == ClassKind.ENUM_CLASS)
        return compilerContext.referenceClass(enumClass.fqNameSafe)?.let {
            it.owner.functions.singleOrNull { f ->
                f.name == Name.identifier("values") && f.valueParameters.isEmpty() && f.extensionReceiverParameter == null
            } ?: throw AssertionError("Enum class does not have single .values() function")
        } ?: error("Couldn't load class $enumClass")
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
        property: SerializableProperty
    ): IrExpression? {
        val nullableSerClass = compilerContext.referenceProperties(SerialEntityNames.wrapIntoNullableExt).single()
        val serializer =
            property.serializableWith?.toClassDescriptor
                ?: if (!property.type.isTypeParameter()) generator.findTypeSerializerOrContext(
                    property.module,
                    property.type,
                    property.descriptor.findPsi()
                ) else null
        return serializerInstance(
            generator,
            dispatchReceiverParameter,
            serializer,
            property.module,
            property.type,
            genericIndex = property.genericIndex
        )
            ?.let { expr -> wrapWithNullableSerializerIfNeeded(property.type, expr, nullableSerClass) }
    }

    private fun IrBuilderWithScope.wrapWithNullableSerializerIfNeeded(
        type: KotlinType,
        expression: IrExpression,
        nullableProp: IrPropertySymbol
    ): IrExpression = if (type.isMarkedNullable) {
        val resultType = type.makeNotNullable()
        val typeArguments = listOf(resultType.toIrType())
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


    fun wrapIrTypeIntoKSerializerIrType(module: ModuleDescriptor, type: IrType, variance: Variance = Variance.INVARIANT): IrType {
        val serializerFqn = getSerializationPackageFqn(SerialEntityNames.KSERIALIZER_CLASS)
        val kSerClass = compilerContext.referenceClass(serializerFqn) ?: error("Couldn't find class $serializerFqn")
        return IrSimpleTypeImpl(
            kSerClass, hasQuestionMark = false, arguments = listOf(
                makeTypeProjection(type, variance)
            ), annotations = emptyList()
        )
    }

    fun IrBuilderWithScope.serializerInstance(
        enclosingGenerator: SerializerIrGenerator,
        dispatchReceiverParameter: IrValueParameter,
        serializerClassOriginal: ClassDescriptor?,
        module: ModuleDescriptor,
        kType: KotlinType,
        genericIndex: Int? = null
    ): IrExpression? = serializerInstance(
        enclosingGenerator,
        serializerClassOriginal,
        module,
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
        kType: KotlinType,
        genericIndex: Int? = null,
        genericGetter: ((Int, KotlinType) -> IrExpression)? = null
    ): IrExpression? {
        val nullableSerClass = compilerContext.referenceProperties(SerialEntityNames.wrapIntoNullableExt).single()
        if (serializerClassOriginal == null) {
            if (genericIndex == null) return null
            return genericGetter?.invoke(genericIndex, kType)
        }
        if (serializerClassOriginal.kind == ClassKind.OBJECT) {
            return irGetObject(serializerClassOriginal)
        }
        fun instantiate(serializer: ClassDescriptor?, type: KotlinType): IrExpression? {
            val expr = serializerInstance(
                enclosingGenerator,
                serializer,
                module,
                type,
                type.genericIndex,
                genericGetter
            ) ?: return null
            return wrapWithNullableSerializerIfNeeded(type, expr, nullableSerClass)
        }

        var serializerClass = serializerClassOriginal
        var args: List<IrExpression>
        var typeArgs: List<IrType?>
        val thisIrType = kType.toIrType()
        var needToCopyAnnotations = false

        when (serializerClassOriginal.classId) {
            polymorphicSerializerId -> {
                needToCopyAnnotations = true
                args = listOf(classReference(kType))
                typeArgs = listOf(thisIrType)
            }
            contextSerializerId -> {
                args = listOf(classReference(kType))
                typeArgs = listOf(thisIrType)

                val hasNewCtxSerCtor =
                    serializerClassOriginal.classId == contextSerializerId && compilerContext.referenceConstructors(serializerClass.fqNameSafe)
                        .any { it.owner.valueParameters.size == 3 }

                if (hasNewCtxSerCtor) {
                    // new signature of context serializer
                    args = args + mutableListOf<IrExpression>().apply {
                        val fallbackDefaultSerializer = findTypeSerializer(module, kType)
                        add(instantiate(fallbackDefaultSerializer, kType) ?: irNull())
                        add(
                            createArrayOfExpression(
                                wrapIrTypeIntoKSerializerIrType(
                                    module,
                                    thisIrType,
                                    variance = Variance.OUT_VARIANCE
                                ),
                                kType.arguments.map {
                                    val argSer = enclosingGenerator.findTypeSerializerOrContext(
                                        module,
                                        it.type,
                                        sourceElement = serializerClassOriginal.findPsi()
                                    )
                                    instantiate(argSer, it.type)!!
                                })
                        )
                    }
                }
            }
            objectSerializerId -> {
                needToCopyAnnotations = true
                args = listOf(irString(kType.serialName()), irGetObject(kType.toClassDescriptor!!))
                typeArgs = listOf(thisIrType)
            }
            sealedSerializerId -> {
                needToCopyAnnotations = true
                args = mutableListOf<IrExpression>().apply {
                    add(irString(kType.serialName()))
                    add(classReference(kType))
                    val (subclasses, subSerializers) = enclosingGenerator.allSealedSerializableSubclassesFor(
                        kType.toClassDescriptor!!,
                        module
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
                            wrapIrTypeIntoKSerializerIrType(module, thisIrType, variance = Variance.OUT_VARIANCE),
                            subSerializers.mapIndexed { i, serializer ->
                                val type = subclasses[i]
                                val expr = serializerInstance(
                                    enclosingGenerator,
                                    serializer,
                                    module,
                                    type,
                                    type.genericIndex
                                ) { _, genericType ->
                                    serializerInstance(
                                        enclosingGenerator,
                                        module.getClassFromSerializationPackage(
                                            SpecialBuiltins.polymorphicSerializer
                                        ),
                                        module,
                                        (genericType.constructor.declarationDescriptor as TypeParameterDescriptor).representativeUpperBound
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
                serializerClass = module.getClassFromInternalSerializationPackage(SpecialBuiltins.enumSerializer)
                args = kType.toClassDescriptor!!.let { enumDesc ->
                    listOf(
                        irString(enumDesc.serialName()),
                        irCall(findEnumValuesMethod(enumDesc))
                    )
                }
                typeArgs = listOf(thisIrType)
            }
            else -> {
                args = kType.arguments.map {
                    val argSer = enclosingGenerator.findTypeSerializerOrContext(
                        module,
                        it.type,
                        sourceElement = serializerClassOriginal.findPsi()
                    )
                    instantiate(argSer, it.type) ?: return null
                }
                typeArgs = kType.arguments.map { it.type.toIrType() }
            }

        }
        if (serializerClassOriginal.classId == referenceArraySerializerId) {
            args = listOf(wrapperClassReference(kType.arguments.single().type)) + args
            typeArgs = listOf(typeArgs[0].makeNotNull()) + typeArgs
        }

        // If KType is interface, .classSerializer always yields PolymorphicSerializer, which may be unavailable for interfaces from other modules
        if (!kType.isInterface() && serializerClassOriginal == kType.toClassDescriptor?.classSerializer && enclosingGenerator !is SerializableCompanionIrGenerator) {
            // This is default type serializer, we can shortcut through Companion.serializer()
            // BUT not during generation of this method itself
            callSerializerFromCompanion(thisIrType, typeArgs, args)?.let { return it }
        }


        val serializable = getSerializableClassDescriptorBySerializer(serializerClass)
        val ctor = if (serializable?.declaredTypeParameters?.isNotEmpty() == true) {
            requireNotNull(
                findSerializerConstructorForTypeArgumentsSerializers(serializerClass)
            ) { "Generated serializer does not have constructor with required number of arguments" }
        } else {
            val constructors = compilerContext.referenceConstructors(serializerClass.fqNameSafe)
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
        return irInvoke(null, ctor, typeArguments = typeArgs, valueArguments = args, returnTypeHint = substitutedReturnType)
    }

    fun collectSerialInfoAnnotations(irClass: IrClass): List<IrConstructorCall> {
        if (!(irClass.isInterface || irClass.descriptor.hasSerializableAnnotation)) return emptyList()
        val annotationByFq: MutableMap<FqName, IrConstructorCall> = irClass.annotations.associateBy { it.symbol.owner.parentAsClass.descriptor.fqNameSafe }.toMutableMap()
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
            if (baseClass.descriptor.isSealed() || baseClass.descriptor.modality == Modality.ABSTRACT) {
                val serializer = findStandardKotlinTypeSerializer(baseClass.module, context.irBuiltIns.unitType.toKotlinType())!!
                // workaround for sealed and classes - the `serializer` function expects non-null serializers, but does not use them, so serializers of any type can be passed
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

    private fun IrBuilderWithScope.wrapperClassReference(classType: KotlinType): IrClassReference {
        if (compilerContext.platform.isJvm()) {
            // "Byte::class" -> "java.lang.Byte::class"
            val wrapperFqName = KotlinBuiltIns.getPrimitiveType(classType)?.let(JvmPrimitiveType::get)?.wrapperFqName
            if (wrapperFqName != null) {
                val wrapperClass = compilerContext.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(wrapperFqName))
                    ?: error("Primitive wrapper class for $classType not found: $wrapperFqName")
                return classReference(wrapperClass.defaultType)
            }
        }
        return classReference(classType)
    }

    private fun findSerializerConstructorForTypeArgumentsSerializers(serializer: ClassDescriptor): IrConstructorSymbol? {
        val serializableImplementationTypeArguments = extractKSerializerArgumentFromImplementation(serializer)?.arguments
            ?: throw AssertionError("Serializer does not implement KSerializer??")

        val typeParamsCount = serializableImplementationTypeArguments.size
        if (typeParamsCount == 0) return null //don't need it
        val constructors = compilerContext.referenceConstructors(serializer.fqNameSafe)

        fun isKSerializer(type: IrType): Boolean {
            val simpleType = type as? IrSimpleType ?: return false
            val classifier = simpleType.classifier as? IrClassSymbol ?: return false
            return classifier.owner.fqNameWhenAvailable == SerialEntityNames.KSERIALIZER_NAME_FQ
        }

        return constructors.singleOrNull {
            it.owner.valueParameters.let { vps -> vps.size == typeParamsCount && vps.all { vp -> isKSerializer(vp.type) } }
        }
    }

    private fun IrConstructor.isSerializationCtor(): Boolean {
        val serialMarker =
            compilerContext.referenceClass(SerializationPackages.internalPackageFqName.child(SerialEntityNames.SERIAL_CTOR_MARKER_NAME))

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
        serializableProperties: List<SerializableProperty>,
        objectToSerialize: IrValueDeclaration,
        localOutput: IrValueDeclaration,
        localSerialDesc: IrValueDeclaration,
        kOutputClass: ClassDescriptor,
        ignoreIndexTo: Int,
        initializerAdapter: (IrExpressionBody) -> IrExpression,
        genericGetter: ((Int, KotlinType) -> IrExpression)?
    ) {

        fun SerializableProperty.irGet(): IrExpression {
            val ownerType = objectToSerialize.symbol.owner.type
            return getProperty(
                irGet(
                    type = ownerType,
                    variable = objectToSerialize.symbol
                ), getIrPropertyFrom(serializableIrClass)
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
                        kOutputClass.referenceFunctionSymbol("${CallingConventions.encode}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}")
                    f to listOf(
                        irGet(localSerialDesc),
                        irInt(index),
                        innerSerial,
                        property.irGet()
                    )
                }, {
                    val f =
                        kOutputClass.referenceFunctionSymbol("${CallingConventions.encode}${it.elementMethodPrefix}${CallingConventions.elementPostfix}")
                    val args: MutableList<IrExpression> = mutableListOf(irGet(localSerialDesc), irInt(index))
                    if (it.elementMethodPrefix != "Unit") args.add(property.irGet())
                    f to args
                },
                genericGetter
            )

            // check for call to .shouldEncodeElementDefault
            val encodeDefaults = property.getIrPropertyFrom(serializableIrClass).getEncodeDefaultAnnotationValue()
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
                    val shouldEncodeFunc = kOutputClass.referenceFunctionSymbol(CallingConventions.shouldEncodeDefault)
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
        val argValue = (arg as? IrGetEnumValue ?: error("Argument of enum constructor expected to implement IrGetEnumValue, got $arg")).symbol.owner.name.toString()
        return when (argValue) {
            "ALWAYS" -> true
            "NEVER" -> false
            else -> error("Unknown EncodeDefaultMode enum value: $argValue")
        }
    }


    fun IrBlockBodyBuilder.formEncodeDecodePropertyCall(
        enclosingGenerator: AbstractSerialGenerator,
        encoder: IrExpression,
        property: SerializableProperty,
        whenHaveSerializer: (serializer: IrExpression, sti: SerialTypeInfo) -> FunctionWithArgs,
        whenDoNot: (sti: SerialTypeInfo) -> FunctionWithArgs,
        genericGetter: ((Int, KotlinType) -> IrExpression)? = null,
        returnTypeHint: IrType? = null
    ): IrExpression {
        val sti = enclosingGenerator.getSerialTypeInfo(property)
        val innerSerial = serializerInstance(
            enclosingGenerator,
            sti.serializer,
            property.module,
            property.type,
            property.genericIndex,
            genericGetter
        )
        val (functionToCall, args: List<IrExpression>) = if (innerSerial != null) whenHaveSerializer(innerSerial, sti) else whenDoNot(sti)
        val typeArgs = if (functionToCall.descriptor.typeParameters.isNotEmpty()) listOf(property.type.toIrType()) else listOf()
        return irInvoke(encoder, functionToCall, typeArguments = typeArgs, valueArguments = args, returnTypeHint = returnTypeHint)
    }

}
