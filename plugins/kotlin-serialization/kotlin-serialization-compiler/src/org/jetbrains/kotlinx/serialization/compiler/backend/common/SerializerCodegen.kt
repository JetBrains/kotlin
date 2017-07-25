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
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.psi.synthetics.findClassDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.resolve.KSerializerDescriptorResolver
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializableProperties

abstract class SerializerCodegen(declaration: KtPureClassOrObject, bindingContext: BindingContext) {
    protected val serializerDescriptor: ClassDescriptor = declaration.findClassDescriptor(bindingContext)
    protected val serializableDescriptor: ClassDescriptor = getSerializableClassDescriptorBySerializer(serializerDescriptor)!!
    protected val serialName: String = serializableDescriptor.fqNameUnsafe.asString()
    protected val properties = SerializableProperties(serializableDescriptor, bindingContext)
    protected val orderedProperties = properties.serializableProperties

    fun generate() {
        check(properties.isExternallySerializable) { "Class ${serializableDescriptor.name} is not externally serializable" }
        generateSerializableClassPropertyIfNeeded()
        val save = generateSaveIfNeeded()
        val load = generateLoadIfNeeded()
        if (save || load)
            generateSerialDesc()
    }

    protected abstract fun generateSerialDesc()

    protected abstract fun generateSerializableClassProperty(property: PropertyDescriptor)

    protected abstract fun generateSave(function: FunctionDescriptor)

    protected abstract fun generateLoad(function: FunctionDescriptor)

    private fun generateSerializableClassPropertyIfNeeded() {
        val property = getPropertyToGenerate(serializerDescriptor, KSerializerDescriptorResolver.SERIAL_DESC_FIELD,
                                             serializerDescriptor::checkSerializableClassPropertyResult)
                       ?: return
        generateSerializableClassProperty(property)
    }

    private fun generateSaveIfNeeded(): Boolean {
        val function = getMemberToGenerate(serializerDescriptor, KSerializerDescriptorResolver.SAVE,
                                           serializerDescriptor::checkSaveMethodResult, serializerDescriptor::checkSaveMethodParameters)
                       ?: return false
        generateSave(function)
        return true
    }

    private fun generateLoadIfNeeded(): Boolean {
        val function = getMemberToGenerate(serializerDescriptor, KSerializerDescriptorResolver.LOAD,
                                           serializerDescriptor::checkLoadMethodResult, serializerDescriptor::checkLoadMethodParameters)
                       ?: return false
        generateLoad(function)
        return true
    }

    fun getPropertyToGenerate(
            classDescriptor: ClassDescriptor,
            name: String,
            isReturnTypeOk: (KotlinType) -> Boolean
    ): PropertyDescriptor? =
            classDescriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                    .singleOrNull { property ->
                        property.kind.let { kind -> kind == CallableMemberDescriptor.Kind.SYNTHESIZED || kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE } &&
                        property.modality != Modality.FINAL &&
                        property.returnType != null &&
                        isReturnTypeOk(property.returnType!!)
                    }
}
