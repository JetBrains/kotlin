/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.common

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.secondaryConstructors
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.types.typeUtil.isUnit
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

    private inline fun ClassDescriptor.shouldHaveSpecificSyntheticMethods(functionPresenceChecker: () -> FunctionDescriptor?) =
        !isInlineClass() && (isAbstractOrSealedSerializableClass() || functionPresenceChecker() != null)

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
            val func =
                serializableDescriptor.unsubstitutedMemberScope.getContributedFunctions(
                    Name.identifier(SerialEntityNames.WRITE_SELF_NAME.toString()),
                    NoLookupLocation.FROM_BACKEND
                ).singleOrNull { function ->
                    function.kind == CallableMemberDescriptor.Kind.SYNTHESIZED &&
                            function.modality == Modality.FINAL &&
                            function.returnType?.isUnit() ?: false
                } ?: return
            generateWriteSelfMethod(func)
        }
    }

    protected abstract fun generateInternalConstructor(constructorDescriptor: ClassConstructorDescriptor)

    protected open fun generateWriteSelfMethod(methodDescriptor: FunctionDescriptor) {

    }
}
