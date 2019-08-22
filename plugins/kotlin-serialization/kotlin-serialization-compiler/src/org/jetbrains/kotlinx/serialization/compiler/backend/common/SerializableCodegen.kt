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

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.secondaryConstructors
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

abstract class SerializableCodegen(
    protected val serializableDescriptor: ClassDescriptor,
    bindingContext: BindingContext
) : AbstractSerialGenerator(bindingContext, serializableDescriptor) {
    protected val properties = bindingContext.serializablePropertiesFor(serializableDescriptor)

    fun generate() {
        generateSyntheticInternalConstructor()
        generateSyntheticMethods()
    }

    private fun generateSyntheticInternalConstructor() {
        val serializerDescriptor = serializableDescriptor.classSerializer ?: return
        if (serializableDescriptor.isAbstractSerializableClass() || serializableDescriptor.isSealedSerializableClass() || SerializerCodegen.getSyntheticLoadMember(serializerDescriptor) != null) {
            val constrDesc = serializableDescriptor.secondaryConstructors.find(ClassConstructorDescriptor::isSerializationCtor) ?: return
            generateInternalConstructor(constrDesc)
        }
    }

    private fun generateSyntheticMethods() {
        val serializerDescriptor = serializableDescriptor.classSerializer ?: return
        if (serializableDescriptor.isAbstractSerializableClass() || serializableDescriptor.isSealedSerializableClass() || SerializerCodegen.getSyntheticSaveMember(serializerDescriptor) != null) {
            val func = KSerializerDescriptorResolver.createWriteSelfFunctionDescriptor(serializableDescriptor)
            generateWriteSelfMethod(func)
        }
    }

    protected abstract fun generateInternalConstructor(constructorDescriptor: ClassConstructorDescriptor)

    protected open fun generateWriteSelfMethod(methodDescriptor: FunctionDescriptor) {

    }
}
