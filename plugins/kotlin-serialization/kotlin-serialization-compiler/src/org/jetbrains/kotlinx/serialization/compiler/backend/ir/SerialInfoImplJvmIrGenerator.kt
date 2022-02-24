/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.ir.addExtensionReceiver
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames

// This doesn't support annotation arguments of type KClass and Array<KClass> because the codegen doesn't compute JVM signatures for
// such cases correctly (because inheriting from annotation classes is prohibited in Kotlin).
// Currently it results in an "accidental override" error where a method with return type KClass conflicts with the one with Class.
class SerialInfoImplJvmIrGenerator(
    private val context: SerializationPluginContext,
    private val moduleFragment: IrModuleFragment,
) : IrBuilderExtension {
    override val compilerContext: SerializationPluginContext
        get() = context

    private val jvmNameClass get() = context.referenceClass(DescriptorUtils.JVM_NAME)!!.owner

    private val javaLangClass = createClass(createPackage("java.lang"), "Class", ClassKind.CLASS)
    private val javaLangType = javaLangClass.starProjectedType

    private val implGenerated = mutableSetOf<IrClass>()
    private val annotationToImpl = mutableMapOf<IrClass, IrClass>()

    fun getImplClass(serialInfoAnnotationClass: IrClass): IrClass =
        annotationToImpl.getOrPut(serialInfoAnnotationClass) {
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
            getter.origin = SERIALIZABLE_PLUGIN_ORIGIN
            // Add JvmName annotation to property getters to force the resulting JVM method name for 'x' be 'x', instead of 'getX',
            // and to avoid having useless bridges for it generated in BridgeLowering.
            // Unfortunately, this results in an extra `@JvmName` annotation in the bytecode, but it shouldn't matter very much.
            getter.annotations += jvmName(property.name.asString())

            val field = property.backingField!!
            field.visibility = DescriptorVisibilities.PRIVATE
            field.origin = SERIALIZABLE_PLUGIN_ORIGIN

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

    override fun IrType.kClassToJClassIfNeeded(): IrType = when {
        this.isKClass() -> javaLangType
        this.isKClassArray() -> compilerContext.irBuiltIns.arrayClass.typeWith(javaLangType)
        else -> this
    }

    override fun kClassExprToJClassIfNeeded(startOffset: Int, endOffset: Int, irExpression: IrExpression): IrExpression {
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
                        putValueArgument(0, IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.stringType, "getJavaClass"))
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
}
