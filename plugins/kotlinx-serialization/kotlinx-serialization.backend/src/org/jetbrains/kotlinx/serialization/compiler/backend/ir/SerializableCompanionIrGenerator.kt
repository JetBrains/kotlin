/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages

class SerializableCompanionIrGenerator(
    val irClass: IrClass,
    val serializableIrClass: IrClass,
    compilerContext: SerializationPluginContext,
) : BaseIrGenerator(irClass, compilerContext) {

    companion object {
        fun getSerializerGetterFunction(serializableIrClass: IrClass): IrSimpleFunction? {
            val irClass =
                if (serializableIrClass.isSerializableObject) serializableIrClass else serializableIrClass.companionObject() ?: return null
            return irClass.findDeclaration<IrSimpleFunction> {
                it.name == SerialEntityNames.SERIALIZER_PROVIDER_NAME
                        && it.valueParameters.size == serializableIrClass.typeParameters.size
                        && it.valueParameters.all { p -> p.type.isKSerializer() }
                        && it.returnType.isKSerializer()
            }
        }

        fun generate(
            irClass: IrClass,
            context: SerializationPluginContext,
        ) {
            val companionDescriptor = irClass
            val serializableClass = getSerializableClassByCompanion(companionDescriptor) ?: return
            if (serializableClass.shouldHaveGeneratedMethodsInCompanion) {
                SerializableCompanionIrGenerator(irClass, getSerializableClassByCompanion(irClass)!!, context).generate()
                irClass.addDefaultConstructorBodyIfAbsent(context)
                irClass.patchDeclarationParents(irClass.parent)
            }
        }
    }

    fun generate() {
        val serializerGetterFunction =
            getSerializerGetterFunction(serializableIrClass)?.takeIf { it.isFromPlugin(compilerContext.afterK2) }
                ?: throw IllegalStateException(
                    "Can't find synthesized 'Companion.serializer()' function to generate, " +
                            "probably clash with user-defined function has occurred"
                )

        if (serializableIrClass.isSerializableObject
            || serializableIrClass.isAbstractOrSealedSerializableClass
            || serializableIrClass.isSerializableEnum()
        ) {
            generateLazySerializerGetter(serializerGetterFunction)
        } else {
            generateSerializerGetter(serializerGetterFunction)
        }

        patchNamedCompanionWithMarkerAnnotation()
    }

    private fun patchNamedCompanionWithMarkerAnnotation() {
        if (serializableIrClass.kind == ClassKind.OBJECT || irClass.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
            return
        }

        val annotationClass = compilerContext.referenceClass(SerializationAnnotations.namedCompanionClassId) ?: return
        val annotationCall = irClass.createAnnotationCallWithoutArgs(annotationClass)
        compilerContext.metadataDeclarationRegistrar.addMetadataVisibleAnnotationsToElement(irClass, annotationCall)
    }

    private fun IrBuilderWithScope.patchSerializableClassWithMarkerAnnotation(serializer: IrClass) {
        if (serializer.kind != ClassKind.OBJECT) {
            return
        }

        val annotationMarkerClass = compilerContext.referenceClass(
            ClassId(
                SerializationPackages.packageFqName,
                Name.identifier(SerialEntityNames.ANNOTATION_MARKER_CLASS)
            )
        ) ?: return

        val irSerializableClass = if (irClass.isCompanion) irClass.parentAsClass else irClass
        val serializableWithAlreadyPresent = irSerializableClass.annotations.any {
            it.constructedClass.fqNameWhenAvailable == annotationMarkerClass.owner.fqNameWhenAvailable
        }
        if (serializableWithAlreadyPresent) return

        val annotationCtor = annotationMarkerClass.constructors.single { it.owner.isPrimary }
        val annotationType = annotationMarkerClass.defaultType

        val annotationCtorCall = IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, annotationType, annotationCtor).apply {
            putValueArgument(
                0,
                createClassReference(
                    serializer.defaultType,
                    startOffset,
                    endOffset
                )
            )
        }

        irSerializableClass.annotations += annotationCtorCall
    }

    fun generateLazySerializerGetter(methodDescriptor: IrSimpleFunction) {
        val serializer = requireNotNull(
            findTypeSerializer(
                compilerContext,
                serializableIrClass.defaultType
            )
        )

        val kSerializerIrClass =
            compilerContext.referenceClass(ClassId(SerializationPackages.packageFqName, SerialEntityNames.KSERIALIZER_NAME))!!.owner
        val targetIrType =
            kSerializerIrClass.defaultType.substitute(mapOf(kSerializerIrClass.typeParameters[0].symbol to compilerContext.irBuiltIns.anyType))

        val property = addLazyValProperty(irClass, targetIrType, SerialEntityNames.CACHED_SERIALIZER_PROPERTY_NAME) {
            val expr = requireNotNull(
                serializerInstance(serializer, compilerContext, serializableIrClass.defaultType)
            )
            patchSerializableClassWithMarkerAnnotation(kSerializerIrClass)
            +expr
        }

        addFunctionBody(methodDescriptor) {
            +irReturn(irInvoke(irGet(it.dispatchReceiverParameter!!), property.getter!!.symbol))
        }
        generateSerializerFactoryIfNeeded(methodDescriptor)
    }

    fun generateSerializerGetter(methodDescriptor: IrSimpleFunction) {
        addFunctionBody(methodDescriptor) { getter ->
            val serializer = requireNotNull(
                findTypeSerializer(
                    compilerContext,
                    serializableIrClass.defaultType
                )
            )
            val args: List<IrExpression> = getter.valueParameters.map { irGet(it) }
            val expr = serializerInstance(
                serializer, compilerContext,
                serializableIrClass.defaultType
            ) { it, _ -> args[it] }
            patchSerializableClassWithMarkerAnnotation(serializer.owner)
            +irReturn(requireNotNull(expr))
        }
        generateSerializerFactoryIfNeeded(methodDescriptor)
    }

    private fun getOrCreateSerializerVarargFactory(): IrSimpleFunction {
        irClass.findDeclaration<IrSimpleFunction> {
            it.name == SerialEntityNames.SERIALIZER_PROVIDER_NAME
                    && it.valueParameters.size == 1
                    && it.valueParameters.first().isVararg
                    && it.returnType.isKSerializer()
                    && it.isFromPlugin(compilerContext.afterK2)
        }?.let { return it }
        val kSerializerStarType = compilerContext.getClassFromRuntime(SerialEntityNames.KSERIALIZER_CLASS).starProjectedType
        val f = irClass.addFunction(
            SerialEntityNames.SERIALIZER_PROVIDER_NAME.asString(),
            kSerializerStarType,
            origin = SERIALIZATION_PLUGIN_ORIGIN
        )
        f.addValueParameter {
            name = Name.identifier("typeParamsSerializers")
            varargElementType = kSerializerStarType
            type = compilerContext.irBuiltIns.arrayClass.typeWith(kSerializerStarType)
            origin = SERIALIZATION_PLUGIN_ORIGIN
        }
        return f.apply { excludeFromJsExport() }
    }

    private fun generateSerializerFactoryIfNeeded(getterDescriptor: IrSimpleFunction) {
        if (!irClass.needSerializerFactory(compilerContext)) return
        val serialFactoryDescriptor = getOrCreateSerializerVarargFactory()
        addFunctionBody(serialFactoryDescriptor) { factory ->
            val kSerializerStarType = factory.returnType
            val array = factory.valueParameters.first()
            val argsSize = serializableIrClass.typeParameters.size
            val arrayGet = compilerContext.irBuiltIns.arrayClass.owner.declarations.filterIsInstance<IrSimpleFunction>()
                .single { it.name.asString() == "get" }

            val serializers: List<IrExpression> = (0 until argsSize).map {
                irInvoke(irGet(array), arrayGet.symbol, irInt(it), typeHint = kSerializerStarType)
            }
            val serializerCall = getterDescriptor.symbol
            val call = irInvoke(
                IrGetValueImpl(startOffset, endOffset, factory.dispatchReceiverParameter!!.symbol),
                serializerCall,
                List(argsSize) { compilerContext.irBuiltIns.anyNType },
                serializers,
                returnTypeHint = kSerializerStarType
            )
            +irReturn(call)
            patchSerializableClassWithMarkerAnnotation(irClass)
        }
    }
}

