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

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIALIZER_PROVIDER_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.getSerializableClassDescriptorByCompanion
import org.jetbrains.kotlinx.serialization.compiler.resolve.isKSerializer

abstract class SerializableCompanionCodegen(
    protected val companionDescriptor: ClassDescriptor,
    bindingContext: BindingContext
) : AbstractSerialGenerator(bindingContext, companionDescriptor) {
    protected val serializableDescriptor: ClassDescriptor = getSerializableClassDescriptorByCompanion(companionDescriptor)!!

    fun generate() {
        val serializerGetterDescriptor = companionDescriptor.unsubstitutedMemberScope.getContributedFunctions(
            SERIALIZER_PROVIDER_NAME,
            NoLookupLocation.FROM_BACKEND
        ).firstOrNull {
            it.valueParameters.size == serializableDescriptor.declaredTypeParameters.size
                    && it.kind == CallableMemberDescriptor.Kind.SYNTHESIZED
                    && it.valueParameters.all { p -> isKSerializer(p.type) }
                    && it.returnType != null && isKSerializer(it.returnType)
        } ?: throw IllegalStateException(
            "Can't find synthesized 'Companion.serializer()' function to generate, " +
                    "probably clash with user-defined function has occurred"
        )
        generateSerializerGetter(serializerGetterDescriptor)
    }

    protected abstract fun generateSerializerGetter(methodDescriptor: FunctionDescriptor)
}