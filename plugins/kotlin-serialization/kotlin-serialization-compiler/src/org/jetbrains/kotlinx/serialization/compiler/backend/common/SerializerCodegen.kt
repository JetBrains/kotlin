/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.common

import org.jetbrains.kotlin.backend.common.CodegenUtil.getMemberToGenerate
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

abstract class SerializerCodegen(
    protected val serializerDescriptor: ClassDescriptor,
    bindingContext: BindingContext
) : AbstractSerialGenerator(bindingContext, serializerDescriptor) {
    val serializableDescriptor: ClassDescriptor = getSerializableClassDescriptorBySerializer(serializerDescriptor)!!
    protected val serialName: String = serializableDescriptor.serialName()
    protected val properties = bindingContext.serializablePropertiesFor(serializableDescriptor)
    protected val serializableProperties = properties.serializableProperties

    private fun checkSerializability() {
        check(properties.isExternallySerializable) {
            "Class ${serializableDescriptor.name} have constructor parameters which are not properties and therefore it is not serializable automatically"
        }
    }

    fun generate() {
        val prop = generateSerializableClassPropertyIfNeeded()
        if (prop)
            generateSerialDesc()
        val save = generateSaveIfNeeded()
        val load = generateLoadIfNeeded()
        generateDescriptorGetterIfNeeded()
        if (!prop && (save || load))
            generateSerialDesc()
        if (serializableDescriptor.declaredTypeParameters.isNotEmpty()) {
            findSerializerConstructorForTypeArgumentsSerializers(serializerDescriptor, onlyIfSynthetic = true)?.let {
                generateGenericFieldsAndConstructor(it)
            }
        }
    }

    private fun generateDescriptorGetterIfNeeded(): Boolean {
        val function = getMemberToGenerate(
            serializerDescriptor, SerialEntityNames.GENERATED_DESCRIPTOR_GETTER.identifier,
            { true }, { true }
        ) ?: return false
        generateChildSerializersGetter(function)
        return true
    }

    protected abstract fun generateChildSerializersGetter(function: FunctionDescriptor)

    protected val generatedSerialDescPropertyDescriptor = getPropertyToGenerate(
        serializerDescriptor, SerialEntityNames.SERIAL_DESC_FIELD,
        serializerDescriptor::checkSerializableClassPropertyResult
    )
    protected val anySerialDescProperty = getProperty(
        serializerDescriptor, SerialEntityNames.SERIAL_DESC_FIELD,
        serializerDescriptor::checkSerializableClassPropertyResult
    ) { true }

    val localSerializersFieldsDescriptors: List<PropertyDescriptor> = findLocalSerializersFieldDescriptors()

    // Can be false if user specified inheritance from KSerializer explicitly
    protected val isGeneratedSerializer = serializerDescriptor.typeConstructor.supertypes.any(::isGeneratedKSerializer)

    private fun findLocalSerializersFieldDescriptors(): List<PropertyDescriptor> {
        val count = serializableDescriptor.declaredTypeParameters.size
        if (count == 0) return emptyList()
        val propNames = (0 until count).map { "${SerialEntityNames.typeArgPrefix}$it" }
        return propNames.mapNotNull { name ->
            getPropertyToGenerate(serializerDescriptor, name) { isKSerializer(it.returnType) }
        }
    }

    protected abstract fun generateSerialDesc()

    protected abstract fun generateGenericFieldsAndConstructor(typedConstructorDescriptor: ClassConstructorDescriptor)

    protected abstract fun generateSerializableClassProperty(property: PropertyDescriptor)

    protected abstract fun generateSave(function: FunctionDescriptor)

    protected abstract fun generateLoad(function: FunctionDescriptor)

    private fun generateSerializableClassPropertyIfNeeded(): Boolean {
        val property = generatedSerialDescPropertyDescriptor
            ?: return false
        checkSerializability()
        generateSerializableClassProperty(property)
        return true
    }

    private fun generateSaveIfNeeded(): Boolean {
        val function = getSyntheticSaveMember(serializerDescriptor) ?: return false
        checkSerializability()
        generateSave(function)
        return true
    }

    private fun generateLoadIfNeeded(): Boolean {
        val function = getSyntheticLoadMember(serializerDescriptor) ?: return false
        checkSerializability()
        generateLoad(function)
        return true
    }

    private fun getPropertyToGenerate(
        classDescriptor: ClassDescriptor,
        name: String,
        isReturnTypeOk: (PropertyDescriptor) -> Boolean
    ): PropertyDescriptor? = getProperty(
        classDescriptor,
        name,
        isReturnTypeOk
    ) { kind ->
        kind == CallableMemberDescriptor.Kind.SYNTHESIZED || kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
    }

    private fun getProperty(
        classDescriptor: ClassDescriptor,
        name: String,
        isReturnTypeOk: (PropertyDescriptor) -> Boolean,
        isKindOk: (CallableMemberDescriptor.Kind) -> Boolean
    ): PropertyDescriptor? = classDescriptor.unsubstitutedMemberScope.getContributedVariables(
        Name.identifier(name),
        NoLookupLocation.FROM_BACKEND
    )
        .singleOrNull { property ->
            isKindOk(property.kind) &&
                    property.returnType != null &&
                    isReturnTypeOk(property)
        }

    protected fun ClassDescriptor.getFuncDesc(funcName: String): Sequence<FunctionDescriptor> =
        unsubstitutedMemberScope.getDescriptorsFiltered { it == Name.identifier(funcName) }.asSequence().filterIsInstance<FunctionDescriptor>()

    companion object {
        fun getSyntheticLoadMember(serializerDescriptor: ClassDescriptor): FunctionDescriptor? = getMemberToGenerate(
            serializerDescriptor, SerialEntityNames.LOAD,
            serializerDescriptor::checkLoadMethodResult, serializerDescriptor::checkLoadMethodParameters
        )

        fun getSyntheticSaveMember(serializerDescriptor: ClassDescriptor): FunctionDescriptor? = getMemberToGenerate(
            serializerDescriptor, SerialEntityNames.SAVE,
            serializerDescriptor::checkSaveMethodResult, serializerDescriptor::checkSaveMethodParameters
        )
    }
}
