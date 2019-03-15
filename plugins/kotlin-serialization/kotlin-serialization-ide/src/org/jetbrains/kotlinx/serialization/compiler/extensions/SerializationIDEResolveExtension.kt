/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.extensions

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.idea.getIfEnabledOn
import org.jetbrains.kotlinx.serialization.idea.runIfEnabledOn
import java.util.*

class SerializationIDEResolveExtension : SerializationResolveExtension() {
    override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> =
        getIfEnabledOn(thisDescriptor) { super.getSyntheticNestedClassNames(thisDescriptor) } ?: emptyList()

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
        getIfEnabledOn(thisDescriptor) { super.getSyntheticFunctionNames(thisDescriptor) } ?: emptyList()

    override fun generateSyntheticClasses(
        thisDescriptor: ClassDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) = runIfEnabledOn(thisDescriptor) {
        super.generateSyntheticClasses(thisDescriptor, name, ctx, declarationProvider, result)
    }

    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? = getIfEnabledOn(thisDescriptor) {
        super.getSyntheticCompanionObjectNameIfNeeded(thisDescriptor)
    }

    override fun addSyntheticSupertypes(thisDescriptor: ClassDescriptor, supertypes: MutableList<KotlinType>) =
        runIfEnabledOn(thisDescriptor) {
            super.addSyntheticSupertypes(thisDescriptor, supertypes)
        }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) = runIfEnabledOn(thisDescriptor) {
        super.generateSyntheticMethods(thisDescriptor, name, bindingContext, fromSupertypes, result)
    }

    override fun generateSyntheticProperties(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: ArrayList<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>
    ) = runIfEnabledOn(thisDescriptor) {
        super.generateSyntheticProperties(thisDescriptor, name, bindingContext, fromSupertypes, result)
    }
}


