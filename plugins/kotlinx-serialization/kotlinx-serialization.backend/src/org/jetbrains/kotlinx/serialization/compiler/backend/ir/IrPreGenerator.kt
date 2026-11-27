/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ValueClassBackendAgnosticApi
import org.jetbrains.kotlin.ir.builders.declarations.v2.irConstructor
import org.jetbrains.kotlin.ir.builders.declarations.v2.irFunction
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.isInlineClass
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.bitMaskSlotCount

/**
 * Generates only specific declarations, but NOT their bodies.
 * This pass is needed to be able to reference these declarations from other generated bodies
 * (e.g. to if we want to reference write$Self() from serialize(), we need to make sure that at least declaration of write$Self is already created.
 *
 * These functions were usually stubbed from descriptors, but since FIR discourages purely synthetic functions,
 * we manually add them here.
 */
class IrPreGenerator(
    val irClass: IrClass,
    compilerContext: SerializationPluginContext,
) : BaseIrGenerator(irClass, compilerContext) {

    private fun generate() {
        preGenerateWriteSelfMethodIfNeeded()
        preGenerateDeserializationConstructorIfNeeded()
    }

    private fun preGenerateWriteSelfMethodIfNeeded() {
        // write$Self in K1 is created only on JVM (see SerializationResolveExtension)
        if (!compilerContext.platform.isJvm()) return
        if (!irClass.shouldHaveGeneratedMethods()) return

        val serializerClass = irClass.findSerializerForGeneratedMethods(compilerContext)?.owner ?: return

        if (!irClass.shouldHaveSpecificSyntheticMethods(isJvm = compilerContext.platform.isJvm()) {
                serializerClass.findPluginGeneratedMethod(SerialEntityNames.SAVE)
            }) return
        if (irClass.findWriteSelfMethod() != null) return
        val serialDescriptorSymbol = compilerContext.getClassFromRuntime(SerialEntityNames.SERIAL_DESCRIPTOR_CLASS)
        val kSerializerSymbol = compilerContext.getClassFromRuntime(SerialEntityNames.KSERIALIZER_CLASS)
        val method = irClass.irFunction {
            name = SerialEntityNames.WRITE_SELF_NAME
            returnType = compilerContext.irBuiltIns.unitType
            visibility = if (irClass.modality == Modality.FINAL) DescriptorVisibilities.INTERNAL else DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            origin = SERIALIZATION_PLUGIN_ORIGIN

            val typeParamsAsArguments = irClass.typeParameters.map {
                typeParameter(it.name.asString(), compilerContext.irBuiltIns.anyNType).defaultType
            }
            // Despite this function should be static in bytecode, registerFunctionAsMetadataVisible needs a correct dispatch receiver
            // to handle function metadata correctly. This dispatchReceiverParameter will be removed later.
            // Its type is the class type with the type parameters of the function instead of the ones of the class.
            dispatchReceiver {
                copyFrom(irClass.thisReceiver!!)
                type = irClass.typeWith(typeParamsAsArguments)
            }

            fun addParameter(name: String, type: IrType) {
                valueParameter {
                    this.name = Name.identifier(name)
                    this.type = type
                    origin = SERIALIZATION_PLUGIN_ORIGIN
                }
            }
            // object
            addParameter("self", irClass.typeWith(typeParamsAsArguments))
            // encoder
            addParameter("output", compilerContext.getClassFromRuntime(SerialEntityNames.STRUCTURE_ENCODER_CLASS).defaultType)
            // descriptor
            addParameter("serialDesc", serialDescriptorSymbol.defaultType)
            // KSerializer<Tn>
            typeParamsAsArguments.forEachIndexed { i, it ->
                addParameter("${SerialEntityNames.typeArgPrefix}$i", kSerializerSymbol.typeWith(it))
            }
        }
        method.excludeFromJsExport()

        compilerContext.finderForBuiltins().findClass(StandardClassIds.Annotations.jvmStatic)
            ?.let { method.annotations += method.createAnnotationCallWithoutArgs(it) }

        compilerContext.metadataDeclarationRegistrar.registerFunctionAsMetadataVisible(method)

        // Make function static in bytecode (see JvmStaticAnnotationLowering.makeStatic).
        method.parameters = method.nonDispatchParameters
    }

    @OptIn(ValueClassBackendAgnosticApi::class)
    private fun preGenerateDeserializationConstructorIfNeeded() {
        if (!irClass.shouldHaveGeneratedMethods()) return
        // do not add synthetic deserialization constructor if .deserialize method is customized
        if (irClass.hasCompanionObjectAsSerializer && irClass.companionObject()
                ?.findPluginGeneratedMethod(SerialEntityNames.LOAD) == null
        ) return
        if (irClass.isInlineClass(treatCompatibleFullValueClassesAsInline = !compilerContext.platform.isJvm())) return
        if (irClass.findSerializableSyntheticConstructor() != null) return
        val markerClassSymbol =
            compilerContext.getClassFromInternalSerializationPackage(SerialEntityNames.SERIAL_CTOR_MARKER_NAME.asString())
        val serializableProperties = serializablePropertiesForIrBackend(irClass).serializableProperties
        val bitMaskSlotsCount = serializableProperties.bitMaskSlotCount()

        val ctor = irClass.irConstructor {
            origin = SERIALIZATION_PLUGIN_ORIGIN
            visibility = if (irClass.modality == Modality.FINAL) DescriptorVisibilities.INTERNAL else DescriptorVisibilities.PUBLIC

            fun addParameter(name: Name, type: IrType) {
                valueParameter {
                    this.name = name
                    this.type = type
                    origin = SERIALIZATION_PLUGIN_ORIGIN
                }
            }
            repeat(bitMaskSlotsCount) {
                addParameter(Name.identifier("seen$it"), compilerContext.irBuiltIns.intType)
            }
            for (prop in serializableProperties) {
                // SerialName can contain illegal identifier characters, so we use original source code name for parameter
                addParameter(prop.originalDescriptorName, prop.type.makeNullableIfNotPrimitive())
            }
            addParameter(SerialEntityNames.dummyParamName, markerClassSymbol.defaultType.makeNullable())
        }.apply { excludeFromJsExport() }
        compilerContext.metadataDeclarationRegistrar.registerConstructorAsMetadataVisible(ctor)
    }

    private fun IrType.makeNullableIfNotPrimitive() =
        if (this.isPrimitiveType(false)) this
        else this.makeNullable()

    companion object {
        fun generate(
            irClass: IrClass,
            compilerContext: SerializationPluginContext
        ) {
            if (!irClass.shouldHaveGeneratedMethods()) return
            IrPreGenerator(irClass, compilerContext).generate()
        }
    }

}
