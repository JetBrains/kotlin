/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.lombok.config.Accessors
import org.jetbrains.kotlin.lombok.config.Getter
import org.jetbrains.kotlin.lombok.utils.createFunction
import org.jetbrains.kotlin.lombok.utils.toPreparedBase
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.typeUtil.isBoolean

class GetterProcessor : Processor {

    override fun contribute(classDescriptor: ClassDescriptor, jClass: JavaClassImpl): Parts {
        val clAccessors = Accessors.getOrNull(classDescriptor)
        val clGetter = Getter.getOrNull(classDescriptor)

        val functions = classDescriptor.unsubstitutedMemberScope.getVariableNames()
            .map {
                classDescriptor.unsubstitutedMemberScope.getContributedVariables(it, NoLookupLocation.FROM_SYNTHETIC_SCOPE).single()
            }.collectWithNotNull { Getter.getOrNull(it) ?: clGetter }
            .mapNotNull { (field, annotation) -> createGetter(classDescriptor, field, annotation, clAccessors) }

        return Parts(functions)
    }

    private fun createGetter(
        classDescriptor: ClassDescriptor,
        field: PropertyDescriptor,
        getter: Getter,
        classLevelAccessors: Accessors?
    ): SimpleFunctionDescriptor? {
        val accessors = Accessors.getOrNull(field) ?: classLevelAccessors ?: Accessors.default

        val functionName =
            if (accessors.fluent) {
                field.name.identifier
            } else {
                val prefix = if (field.type.isPrimitiveBoolean()) "is" else "get"
                prefix + toPreparedBase(field.name.identifier)
            }
        return createFunction(
            classDescriptor,
            Name.identifier(functionName),
            emptyList(),
            field.returnType,
            visibility = getter.visibility
        )
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <E, R> Collection<E>.collectWithNotNull(f: (E) -> R?): List<Pair<E, R>> =
        map { it to f(it) }.filter { it.second != null } as List<Pair<E, R>>

    private fun KotlinType.isPrimitiveBoolean(): Boolean = this is SimpleTypeMarker && isBoolean()  //todo

}
