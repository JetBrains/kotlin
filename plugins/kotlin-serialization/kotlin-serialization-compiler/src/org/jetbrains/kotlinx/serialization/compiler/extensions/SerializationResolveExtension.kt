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

package org.jetbrains.kotlinx.serialization.compiler.extensions

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.platform
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.serialInfoFqName
import java.util.*

open class SerializationResolveExtension : SyntheticResolveExtension {
    override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> = when {
        thisDescriptor.annotations.hasAnnotation(serialInfoFqName) && thisDescriptor.platform?.isJvm() == true -> listOf(SerialEntityNames.IMPL_NAME)
        thisDescriptor.isInternalSerializable && !thisDescriptor.hasCompanionObjectAsSerializer ->
            listOf(SerialEntityNames.SERIALIZER_CLASS_NAME)
        else -> listOf()
    }

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> = when {
        thisDescriptor.isCompanionObject && getSerializableClassDescriptorByCompanion(thisDescriptor) != null ->
            listOf(SerialEntityNames.SERIALIZER_PROVIDER_NAME)
        else -> emptyList()
    }

    override fun generateSyntheticClasses(
        thisDescriptor: ClassDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {
        if (thisDescriptor.annotations.hasAnnotation(serialInfoFqName) && name == SerialEntityNames.IMPL_NAME)
            result.add(KSerializerDescriptorResolver.addSerialInfoImplClass(thisDescriptor, declarationProvider, ctx))
        else if (thisDescriptor.isInternalSerializable && name == SerialEntityNames.SERIALIZER_CLASS_NAME &&
            result.none { it.name == SerialEntityNames.SERIALIZER_CLASS_NAME }
        )
            result.add(KSerializerDescriptorResolver.addSerializerImplClass(thisDescriptor, declarationProvider, ctx))
        return
    }

    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? =
        if (thisDescriptor.shouldHaveGeneratedMethodsInCompanion) SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        else null

    override fun addSyntheticSupertypes(thisDescriptor: ClassDescriptor, supertypes: MutableList<KotlinType>) {
        KSerializerDescriptorResolver.addSerialInfoSuperType(thisDescriptor, supertypes)
        KSerializerDescriptorResolver.addSerializerSupertypes(thisDescriptor, supertypes)
    }

    override fun generateSyntheticSecondaryConstructors(
        thisDescriptor: ClassDescriptor,
        bindingContext: BindingContext,
        result: MutableCollection<ClassConstructorDescriptor>
    ) {
        if (thisDescriptor.isInternalSerializable) {
            result.add(KSerializerDescriptorResolver.createLoadConstructorDescriptor(thisDescriptor, bindingContext))
        }
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        KSerializerDescriptorResolver.generateSerializerMethods(thisDescriptor, fromSupertypes, name, result)
        KSerializerDescriptorResolver.generateCompanionObjectMethods(thisDescriptor, name, result)
    }

    override fun generateSyntheticProperties(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: ArrayList<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>
    ) {
        KSerializerDescriptorResolver.generateDescriptorsForAnnotationImpl(thisDescriptor, fromSupertypes, result)
        KSerializerDescriptorResolver.generateSerializerProperties(thisDescriptor, fromSupertypes, name, result)
    }
}