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
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.psi.synthetics.findClassDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.KSerializerDescriptorResolver.createTypedSerializerConstructorDescriptor

abstract class SerializerCodegen(
    protected val serializerDescriptor: ClassDescriptor,
    bindingContext: BindingContext
) {
    //    protected val serializerDescriptor: ClassDescriptor = declaration.findClassDescriptor(bindingContext)
    protected val serializableDescriptor: ClassDescriptor = getSerializableClassDescriptorBySerializer(serializerDescriptor)!!
    protected val serialName: String = serializableDescriptor.annotations.serialNameValue ?: serializableDescriptor.fqNameUnsafe.asString()
    protected val properties = SerializableProperties(serializableDescriptor, bindingContext)
    protected val orderedProperties = properties.serializableProperties

    fun generate() {
        check(properties.isExternallySerializable) { "Class ${serializableDescriptor.name} is not externally serializable" }
        generateSerialDesc()
        val prop = generateSerializableClassPropertyIfNeeded()
        val save = generateSaveIfNeeded()
        val load = generateLoadIfNeeded()
//        if (save || load || prop)
//            generateSerialDesc()
        if (serializableDescriptor.declaredTypeParameters.isNotEmpty() && typedSerializerConstructorNotDeclared()) {
            generateGenericFieldsAndConstructor(createTypedSerializerConstructorDescriptor(serializerDescriptor, serializableDescriptor))
        }
    }

    // checks if user didn't declared constructor (KSerializer<T0>, KSerializer<T1>...) on a KSerializer<T<T0, T1...>>
    private fun typedSerializerConstructorNotDeclared(): Boolean {
        val serializableImplementationTypeArguments = extractKSerializerArgumentFromImplementation(serializerDescriptor)?.arguments
                ?: throw AssertionError("Serializer does not implement KSerializer??")

        val typeParamsCount = serializableImplementationTypeArguments.size
        if (typeParamsCount == 0) return false //don't need it
        val ctors = serializerDescriptor.constructors
        val found =
            ctors.any {
                it.valueParameters.size == typeParamsCount && it.valueParameters.foldIndexed(false) { index, flag, parameterDescriptor ->
                    val type = parameterDescriptor.type
                    flag || (isKSerializer(type) && type.arguments.first() == serializableImplementationTypeArguments[index])
                }
            }
        return !found
    }

    protected val generatedSerialDescPropertyDescriptor = getPropertyToGenerate(
        serializerDescriptor, SerialEntityNames.SERIAL_DESC_FIELD,
                                                                       serializerDescriptor::checkSerializableClassPropertyResult)
    protected val anySerialDescProperty = getProperty(
        serializerDescriptor, SerialEntityNames.SERIAL_DESC_FIELD,
        serializerDescriptor::checkSerializableClassPropertyResult
    ) { true }

    protected abstract fun generateSerialDesc()

    protected abstract fun generateGenericFieldsAndConstructor(typedConstructorDescriptor: ConstructorDescriptor)

    protected abstract fun generateSerializableClassProperty(property: PropertyDescriptor)

    protected abstract fun generateSave(function: FunctionDescriptor)

    protected abstract fun generateLoad(function: FunctionDescriptor)

    private fun generateSerializableClassPropertyIfNeeded(): Boolean {
        val property = generatedSerialDescPropertyDescriptor
                       ?: return false
        generateSerializableClassProperty(property)
        return true
    }

    private fun generateSaveIfNeeded(): Boolean {
        val function = getMemberToGenerate(serializerDescriptor, SerialEntityNames.SAVE,
                                           serializerDescriptor::checkSaveMethodResult, serializerDescriptor::checkSaveMethodParameters)
                       ?: return false
        generateSave(function)
        return true
    }

    private fun generateLoadIfNeeded(): Boolean {
        val function = getMemberToGenerate(serializerDescriptor, SerialEntityNames.LOAD,
                                           serializerDescriptor::checkLoadMethodResult, serializerDescriptor::checkLoadMethodParameters)
                       ?: return false
        generateLoad(function)
        return true
    }

    fun getPropertyToGenerate(
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

    fun getProperty(
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
                    property.modality != Modality.FINAL &&
                    property.returnType != null &&
                    isReturnTypeOk(property)
        }

    protected fun ClassDescriptor.getFuncDesc(funcName: String): Sequence<FunctionDescriptor> =
            unsubstitutedMemberScope.getDescriptorsFiltered { it == Name.identifier(funcName) }.asSequence().filterIsInstance<FunctionDescriptor>()
}
