/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.common

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.secondaryConstructors
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.VersionReader
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

abstract class SerializableCodegen(
    protected val serializableDescriptor: ClassDescriptor,
    bindingContext: BindingContext
) : AbstractSerialGenerator(bindingContext, serializableDescriptor) {
    protected val properties = bindingContext.serializablePropertiesFor(serializableDescriptor)
    protected val staticDescriptor = serializableDescriptor.declaredTypeParameters.isEmpty()

    private val fieldMissingOptimizationVersion = ApiVersion.parse("1.1")!!
    protected val useFieldMissingOptimization = canUseFieldMissingOptimization()

    fun generate() {
        generateSyntheticInternalConstructor()
        generateSyntheticMethods()
    }

    private inline fun ClassDescriptor.shouldHaveSpecificSyntheticMethods(functionPresenceChecker: () -> FunctionDescriptor?) =
        !isInline && (isAbstractSerializableClass() || isSealedSerializableClass() || functionPresenceChecker() != null)

    private fun generateSyntheticInternalConstructor() {
        val serializerDescriptor = serializableDescriptor.classSerializer ?: return
        if (serializableDescriptor.shouldHaveSpecificSyntheticMethods { SerializerCodegen.getSyntheticLoadMember(serializerDescriptor) }) {
            val constrDesc = serializableDescriptor.secondaryConstructors.find(ClassConstructorDescriptor::isSerializationCtor) ?: return
            generateInternalConstructor(constrDesc)
        }
    }

    private fun generateSyntheticMethods() {
        val serializerDescriptor = serializableDescriptor.classSerializer ?: return
        if (serializableDescriptor.shouldHaveSpecificSyntheticMethods { SerializerCodegen.getSyntheticSaveMember(serializerDescriptor) }) {
            val func = KSerializerDescriptorResolver.createWriteSelfFunctionDescriptor(serializableDescriptor)
            generateWriteSelfMethod(func)
        }
    }

    protected fun getGoldenMask(): Int {
        var goldenMask = 0
        var requiredBit = 1
        for (property in properties.serializableProperties) {
            if (!property.optional) {
                goldenMask = goldenMask or requiredBit
            }
            requiredBit = requiredBit shl 1
        }
        return goldenMask
    }

    protected fun getGoldenMaskList(): List<Int> {
        val maskSlotCount = properties.serializableProperties.bitMaskSlotCount()
        val goldenMaskList = MutableList(maskSlotCount) { 0 }

        for (i in properties.serializableProperties.indices) {
            if (!properties.serializableProperties[i].optional) {
                val slotNumber = i / 32
                val bitInSlot = i % 32
                goldenMaskList[slotNumber] = goldenMaskList[slotNumber] or (1 shl bitInSlot)
            }
        }
        return goldenMaskList
    }

    private fun canUseFieldMissingOptimization(): Boolean {
        val implementationVersion = VersionReader.getVersionsForCurrentModuleFromContext(
            serializableDescriptor.module,
            bindingContext
        )?.implementationVersion
        return if (implementationVersion != null) implementationVersion >= fieldMissingOptimizationVersion else false
    }

    protected abstract fun generateInternalConstructor(constructorDescriptor: ClassConstructorDescriptor)

    protected open fun generateWriteSelfMethod(methodDescriptor: FunctionDescriptor) {

    }
}
