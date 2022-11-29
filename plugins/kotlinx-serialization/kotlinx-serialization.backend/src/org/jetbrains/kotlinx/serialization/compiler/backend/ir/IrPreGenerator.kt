/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.name.Name
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
        if (!irClass.isInternalSerializable) return
        val serializerDescriptor = irClass.classSerializer(compilerContext)?.owner ?: return
        if (!irClass.shouldHaveSpecificSyntheticMethods {
                serializerDescriptor.findPluginGeneratedMethod(
                    SerialEntityNames.SAVE,
                    compilerContext.afterK2
                )
            }) return
        if (irClass.findWriteSelfMethod() != null) return
        val method = irClass.addFunction {
            name = SerialEntityNames.WRITE_SELF_NAME
            returnType = compilerContext.irBuiltIns.unitType
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            origin = SERIALIZATION_PLUGIN_ORIGIN
        }
        method.apply {
            dispatchReceiverParameter = null // function is static
        }

        val typeParams = irClass.typeParameters.map {
            method.addTypeParameter(
                it.name.asString(), compilerContext.irBuiltIns.anyNType
            )
        }
        val typeParamsAsArguments = typeParams.map { it.defaultType }

        // object
        method.addValueParameter(
            Name.identifier("self"), irClass.typeWith(typeParamsAsArguments),
            SERIALIZATION_PLUGIN_ORIGIN
        )
        // encoder
        method.addValueParameter(
            Name.identifier("output"),
            compilerContext.getClassFromRuntime(SerialEntityNames.STRUCTURE_ENCODER_CLASS).defaultType,
            SERIALIZATION_PLUGIN_ORIGIN
        )
        // descriptor
        val serialDescriptorSymbol = compilerContext.getClassFromRuntime(SerialEntityNames.SERIAL_DESCRIPTOR_CLASS)
        method.addValueParameter(
            Name.identifier("serialDesc"), serialDescriptorSymbol.defaultType,
            SERIALIZATION_PLUGIN_ORIGIN
        )
        // KSerializer<Tn>
        val kSerializerSymbol = compilerContext.getClassFromRuntime(SerialEntityNames.KSERIALIZER_CLASS)
        typeParamsAsArguments.forEachIndexed { i, it ->
            method.addValueParameter(
                Name.identifier("${SerialEntityNames.typeArgPrefix}$i"),
                kSerializerSymbol.typeWith(it),
                SERIALIZATION_PLUGIN_ORIGIN
            )
        }
    }

    private fun preGenerateDeserializationConstructorIfNeeded() {
        if (!irClass.isInternalSerializable) return
        // do not add synthetic deserialization constructor if .deserialize method is customized
        if (irClass.hasCompanionObjectAsSerializer && irClass.companionObject()
                ?.findPluginGeneratedMethod(SerialEntityNames.LOAD, compilerContext.afterK2) == null
        ) return
        if (irClass.isValue) return
        if (irClass.findSerializableSyntheticConstructor() != null) return
        val ctor = irClass.addConstructor {
            origin = SERIALIZATION_PLUGIN_ORIGIN
            visibility = DescriptorVisibilities.PUBLIC
        }
        val markerClassSymbol =
            compilerContext.getClassFromInternalSerializationPackage(SerialEntityNames.SERIAL_CTOR_MARKER_NAME.asString())
        val serializableProperties = serializablePropertiesForIrBackend(irClass).serializableProperties
        val bitMaskSlotsCount = serializableProperties.bitMaskSlotCount()

        repeat(bitMaskSlotsCount) {
            ctor.addValueParameter(Name.identifier("seen$it"), compilerContext.irBuiltIns.intType, SERIALIZATION_PLUGIN_ORIGIN)
        }

        for (prop in serializableProperties) {
            ctor.addValueParameter(prop.name, prop.type.makeNullableIfNotPrimitive(), SERIALIZATION_PLUGIN_ORIGIN)
        }

        ctor.addValueParameter(SerialEntityNames.dummyParamName, markerClassSymbol.defaultType, SERIALIZATION_PLUGIN_ORIGIN)
    }

    private fun IrType.makeNullableIfNotPrimitive() =
        if (this.isPrimitiveType(false)) this
        else this.makeNullable()

    companion object {
        fun generate(
            irClass: IrClass,
            compilerContext: SerializationPluginContext
        ) {
            if (!irClass.isInternalSerializable) return
            IrPreGenerator(irClass, compilerContext).generate()
        }
    }

}
