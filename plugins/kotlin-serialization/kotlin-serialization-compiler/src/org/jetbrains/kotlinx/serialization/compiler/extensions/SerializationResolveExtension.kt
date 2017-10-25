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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.resolve.KSerializerDescriptorResolver
import org.jetbrains.kotlinx.serialization.compiler.resolve.isInternalSerializable
import org.jetbrains.kotlinx.serialization.compiler.resolve.serialInfoFqName
import java.util.*

class SerializationResolveExtension : SyntheticResolveExtension {
    override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> = when {
        thisDescriptor.annotations.hasAnnotation(serialInfoFqName) -> listOf(KSerializerDescriptorResolver.IMPL_NAME)
        thisDescriptor.isInternalSerializable -> listOf(KSerializerDescriptorResolver.SERIALIZER_CLASS_NAME)
        else -> listOf()
    }

    override fun generateSyntheticClasses(thisDescriptor: ClassDescriptor, name: Name, ctx: LazyClassContext, declarationProvider: ClassMemberDeclarationProvider, result: MutableSet<ClassDescriptor>) {
        if (thisDescriptor.annotations.hasAnnotation(serialInfoFqName) && name == KSerializerDescriptorResolver.IMPL_NAME)
            result.add(KSerializerDescriptorResolver.addSerialInfoImplClass(thisDescriptor, declarationProvider, ctx))
        else if (thisDescriptor.isInternalSerializable && name == KSerializerDescriptorResolver.SERIALIZER_CLASS_NAME)
            result.add(KSerializerDescriptorResolver.addSerializerImplClass(thisDescriptor, declarationProvider, ctx))
        return
    }

    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? =
            if (thisDescriptor.isInternalSerializable) SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
            else null

    override fun addSyntheticSupertypes(thisDescriptor: ClassDescriptor, supertypes: MutableList<KotlinType>) {
        KSerializerDescriptorResolver.addSerialInfoSuperType(thisDescriptor, supertypes)
        KSerializerDescriptorResolver.addSerializerSupertypes(thisDescriptor, supertypes)
    }

    override fun generateSyntheticMethods(thisDescriptor: ClassDescriptor, name: Name, fromSupertypes: List<SimpleFunctionDescriptor>, result: MutableCollection<SimpleFunctionDescriptor>) {
        KSerializerDescriptorResolver.generateSerializerMethods(thisDescriptor, fromSupertypes, name, result)
        KSerializerDescriptorResolver.generateCompanionObjectMethods(thisDescriptor, fromSupertypes, name, result)
    }

    override fun generateSyntheticProperties(thisDescriptor: ClassDescriptor, name: Name, fromSupertypes: ArrayList<PropertyDescriptor>, result: MutableSet<PropertyDescriptor>) {
        KSerializerDescriptorResolver.generateDescriptorsForAnnotationImpl(thisDescriptor, name, fromSupertypes, result)
        KSerializerDescriptorResolver.generateSerializerProperties(thisDescriptor, fromSupertypes, name, result)
    }
}