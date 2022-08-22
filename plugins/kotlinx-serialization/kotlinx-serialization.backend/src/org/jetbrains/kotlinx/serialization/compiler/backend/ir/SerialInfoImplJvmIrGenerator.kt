/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.ir.addExtensionReceiver
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames

// This doesn't support annotation arguments of type KClass and Array<KClass> because the codegen doesn't compute JVM signatures for
// such cases correctly (because inheriting from annotation classes is prohibited in Kotlin).
// Currently it results in an "accidental override" error where a method with return type KClass conflicts with the one with Class.
@OptIn(ObsoleteDescriptorBasedAPI::class)
class SerialInfoImplJvmIrGenerator(
    private val context: SerializationPluginContext,
    private val moduleFragment: IrModuleFragment,
) : IrBuilderWithPluginContext {
    override val compilerContext: SerializationPluginContext
        get() = context

    private val jvmNameClass get() = context.referenceClass(ClassId.topLevel(DescriptorUtils.JVM_NAME))!!.owner

    private val javaLangClass = createClass(createPackage("java.lang"), "Class", ClassKind.CLASS)
    private val javaLangType = javaLangClass.starProjectedType

    private val implGenerated = mutableSetOf<IrClass>()
    private val annotationToImpl = mutableMapOf<IrClass, IrClass>()

    fun getImplClass(serialInfoAnnotationClass: IrClass): IrClass =
        annotationToImpl.getOrPut(serialInfoAnnotationClass) {
            @OptIn(FirIncompatiblePluginAPI::class)
            val implClassSymbol = context.referenceClass(serialInfoAnnotationClass.kotlinFqName.child(SerialEntityNames.IMPL_NAME))
            implClassSymbol!!.owner.apply(this::generate)
        }

    fun generate(irClass: IrClass) {
        if (!implGenerated.add(irClass)) return

        val properties = irClass.declarations.filterIsInstance<IrProperty>()
        if (properties.isEmpty()) return

        val startOffset = UNDEFINED_OFFSET
        val endOffset = UNDEFINED_OFFSET

        val ctor = irClass.addConstructor {
            visibility = DescriptorVisibilities.PUBLIC
        }
        val ctorBody = context.irFactory.createBlockBody(
            startOffset, endOffset, listOf(
                IrDelegatingConstructorCallImpl(
                    startOffset, endOffset, context.irBuiltIns.unitType, context.irBuiltIns.anyClass.constructors.single(),
                    typeArgumentsCount = 0, valueArgumentsCount = 0
                )
            )
        )
        ctor.body = ctorBody

        for (property in properties) {
            generateSimplePropertyWithBackingField(property.descriptor, irClass, Name.identifier("_" + property.name.asString()))

            val getter = property.getter!!
            getter.origin = SERIALIZATION_PLUGIN_ORIGIN
            // Add JvmName annotation to property getters to force the resulting JVM method name for 'x' be 'x', instead of 'getX',
            // and to avoid having useless bridges for it generated in BridgeLowering.
            // Unfortunately, this results in an extra `@JvmName` annotation in the bytecode, but it shouldn't matter very much.
            getter.annotations += jvmName(property.name.asString())

            val field = property.backingField!!
            field.visibility = DescriptorVisibilities.PRIVATE
            field.origin = SERIALIZATION_PLUGIN_ORIGIN

            val parameter = ctor.addValueParameter(property.name.asString(), field.type)
            ctorBody.statements += IrSetFieldImpl(
                startOffset, endOffset, field.symbol,
                IrGetValueImpl(startOffset, endOffset, irClass.thisReceiver!!.symbol),
                IrGetValueImpl(startOffset, endOffset, parameter.symbol),
                context.irBuiltIns.unitType,
            )
        }
    }

    private fun jvmName(name: String): IrConstructorCall =
        IrConstructorCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, jvmNameClass.defaultType, jvmNameClass.constructors.single().symbol,
            typeArgumentsCount = 0, constructorTypeArgumentsCount = 0, valueArgumentsCount = 1,
        ).apply {
            putValueArgument(0, IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.stringType, name))
        }

    @FirIncompatiblePluginAPI
    fun KotlinType.toIrType() = compilerContext.typeTranslator.translateType(this)

    private fun IrType.kClassToJClassIfNeeded(): IrType = when {
        this.isKClass() -> javaLangType
        this.isKClassArray() -> compilerContext.irBuiltIns.arrayClass.typeWith(javaLangType)
        else -> this
    }

    private fun kClassExprToJClassIfNeeded(startOffset: Int, endOffset: Int, irExpression: IrExpression): IrExpression {
        val getterSymbol = kClassJava.owner.getter!!.symbol
        return IrCallImpl(
            startOffset, endOffset,
            javaLangClass.starProjectedType,
            getterSymbol,
            typeArgumentsCount = getterSymbol.owner.typeParameters.size,
            valueArgumentsCount = 0,
            origin = IrStatementOrigin.GET_PROPERTY
        ).apply {
            this.extensionReceiver = irExpression
        }
    }

    private val jvmName: IrClassSymbol = createClass(createPackage("kotlin.jvm"), "JvmName", ClassKind.ANNOTATION_CLASS) { klass ->
        klass.addConstructor().apply {
            addValueParameter("name", context.irBuiltIns.stringType)
        }
    }

    private val kClassJava: IrPropertySymbol =
        IrFactoryImpl.buildProperty {
            name = Name.identifier("java")
        }.apply {
            parent = createClass(createPackage("kotlin.jvm"), "JvmClassMappingKt", ClassKind.CLASS).owner
            addGetter().apply {
                annotations = listOf(
                    IrConstructorCallImpl.fromSymbolOwner(jvmName.typeWith(), jvmName.constructors.single()).apply {
                        putValueArgument(
                            0,
                            IrConstImpl.string(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                context.irBuiltIns.stringType,
                                "getJavaClass"
                            )
                        )
                    }
                )
                addExtensionReceiver(context.irBuiltIns.kClassClass.starProjectedType)
                returnType = javaLangClass.starProjectedType
            }
        }.symbol

    private fun IrType.isKClassArray() =
        this is IrSimpleType && isArray() && arguments.single().typeOrNull?.isKClass() == true

    private fun createPackage(packageName: String): IrPackageFragment =
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
            moduleFragment.descriptor,
            FqName(packageName)
        )

    private fun createClass(
        irPackage: IrPackageFragment,
        shortName: String,
        classKind: ClassKind,
        block: (IrClass) -> Unit = {}
    ): IrClassSymbol = IrFactoryImpl.buildClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = Modality.FINAL
    }.apply {
        parent = irPackage
        createImplicitParameterDeclarationWithWrappedDescriptor()
        block(this)
    }.symbol

    private inline fun <reified T : IrDeclaration> IrClass.searchForDeclaration(descriptor: DeclarationDescriptor): T? {
        return declarations.singleOrNull { it.descriptor == descriptor } as? T
    }

    private fun generateSimplePropertyWithBackingField(
        propertyDescriptor: PropertyDescriptor,
        propertyParent: IrClass,
        fieldName: Name = propertyDescriptor.name,
    ): IrProperty {
        val irProperty = propertyParent.searchForDeclaration(propertyDescriptor) ?: run {
            with(propertyDescriptor) {
                propertyParent.factory.createProperty(
                    propertyParent.startOffset,
                    propertyParent.endOffset,
                    SERIALIZATION_PLUGIN_ORIGIN,
                    IrPropertySymbolImpl(propertyDescriptor),
                    name,
                    visibility,
                    modality,
                    isVar,
                    isConst,
                    isLateInit,
                    isDelegated,
                    isExternal
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
            @OptIn(FirIncompatiblePluginAPI::class)// should be called only with old FE
            originProperty.factory.createField(
                originProperty.startOffset,
                originProperty.endOffset,
                SERIALIZATION_PLUGIN_ORIGIN,
                IrFieldSymbolImpl(propertyDescriptor),
                name,
                type.toIrType(),
                visibility,
                !isVar,
                isEffectivelyExternal(),
                dispatchReceiverParameter == null
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
                @OptIn(FirIncompatiblePluginAPI::class) // should never be called after FIR frontend
                property.factory.createFunction(
                    fieldSymbol.owner.startOffset,
                    fieldSymbol.owner.endOffset,
                    SERIALIZATION_PLUGIN_ORIGIN, IrSimpleFunctionSymbolImpl(descriptor),
                    name, visibility, modality, returnType!!.toIrType(),
                    isInline, isEffectivelyExternal(), isTailrec, isSuspend, isOperator, isInfix, isExpect
                )
            }.also { f ->
                generateOverriddenFunctionSymbols(f, compilerContext.symbolTable)
                f.createParameterDeclarations(descriptor)
                @OptIn(FirIncompatiblePluginAPI::class) // should never be called after FIR frontend
                f.returnType = descriptor.returnType!!.toIrType()
                f.correspondingPropertySymbol = fieldSymbol.owner.correspondingPropertySymbol
            }
        }

        irAccessor.body = when (isGetter) {
            true -> generateDefaultGetterBody(irAccessor)
            false -> generateDefaultSetterBody(irAccessor)
        }

        return irAccessor
    }

    private fun generateDefaultGetterBody(
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val irProperty =
            irAccessor.correspondingPropertySymbol?.owner ?: error("Expected corresponding property for accessor ${irAccessor.render()}")

        val startOffset = irAccessor.startOffset
        val endOffset = irAccessor.endOffset
        val irBody = irAccessor.factory.createBlockBody(startOffset, endOffset)

        val receiver = generateReceiverExpressionForFieldAccess(irAccessor.dispatchReceiverParameter!!.symbol)

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
                    if (propertyIrType.isKClass()) {
                        irAccessor.returnType = irAccessor.returnType.kClassToJClassIfNeeded()
                        kClassExprToJClassIfNeeded(startOffset, endOffset, it)
                    } else it
                }
            )
        )
        return irBody
    }

    private fun generateDefaultSetterBody(
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val irProperty =
            irAccessor.correspondingPropertySymbol?.owner ?: error("Expected corresponding property for accessor ${irAccessor.render()}")
        val startOffset = irAccessor.startOffset
        val endOffset = irAccessor.endOffset
        val irBody = irAccessor.factory.createBlockBody(startOffset, endOffset)

        val receiver = generateReceiverExpressionForFieldAccess(irAccessor.dispatchReceiverParameter!!.symbol)

        val irValueParameter = irAccessor.valueParameters.single()
        irBody.statements.add(
            IrSetFieldImpl(
                startOffset, endOffset,
                irProperty.backingField?.symbol ?: error("Property ${irProperty.render()} expected to have backing field"),
                receiver,
                IrGetValueImpl(startOffset, endOffset, irValueParameter.type, irValueParameter.symbol),
                compilerContext.irBuiltIns.unitType
            )
        )
        return irBody
    }

    private fun generateReceiverExpressionForFieldAccess(
        ownerSymbol: IrValueSymbol
    ): IrExpression = IrGetValueImpl(
        ownerSymbol.owner.startOffset, ownerSymbol.owner.endOffset,
        ownerSymbol
    )

    private fun IrFunction.createParameterDeclarations(
        descriptor: FunctionDescriptor,
        overwriteValueParameters: Boolean = false,
        copyTypeParameters: Boolean = true
    ) {
        val function = this
        fun irValueParameter(descriptor: ParameterDescriptor): IrValueParameter = with(descriptor) {
            @OptIn(FirIncompatiblePluginAPI::class) // should never be called after FIR frontend
            factory.createValueParameter(
                function.startOffset, function.endOffset, SERIALIZATION_PLUGIN_ORIGIN, IrValueParameterSymbolImpl(this),
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

    private fun IrFunction.copyTypeParamsFromDescriptor(descriptor: FunctionDescriptor) {
        val newTypeParameters = descriptor.typeParameters.map {
            factory.createTypeParameter(
                startOffset, endOffset,
                SERIALIZATION_PLUGIN_ORIGIN,
                IrTypeParameterSymbolImpl(it),
                it.name, it.index, it.isReified, it.variance
            ).also { typeParameter ->
                typeParameter.parent = this
            }
        }
        @OptIn(FirIncompatiblePluginAPI::class) // should never be called after FIR frontend
        newTypeParameters.forEach { typeParameter ->
            typeParameter.superTypes = typeParameter.descriptor.upperBounds.map { it.toIrType() }
        }

        typeParameters = newTypeParameters
    }
}
