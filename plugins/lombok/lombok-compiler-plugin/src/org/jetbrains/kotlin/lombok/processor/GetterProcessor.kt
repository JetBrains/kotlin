/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.lombok.utils.LombokAnnotationNames
import org.jetbrains.kotlin.lombok.utils.createFunction
import org.jetbrains.kotlin.lombok.utils.getVisibility
import org.jetbrains.kotlin.lombok.utils.toPreparedBase
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberType

class GetterProcessor : Processor {

    override fun contribute(classDescriptor: ClassDescriptor, jClass: JavaClassImpl): Parts {

        val functions = classDescriptor.unsubstitutedMemberScope.getVariableNames()
            .map {
                classDescriptor.unsubstitutedMemberScope.getContributedVariables(it, NoLookupLocation.FROM_SYNTHETIC_SCOPE).single()
            }.collectWithNotNull { it.annotations.findAnnotation(LombokAnnotationNames.GETTER) }
            .mapNotNull { (field, annotation) -> createGetter(classDescriptor, field, annotation) }

//        val functions = jClass.fields.collectWithNotNull { it.findAnnotation(LombokAnnotationNames.GETTER) }.mapNotNull { (field, annotation) ->
//            createGetter(classDescriptor, field, annotation)
//        }
        return Parts(functions)
    }

    private fun createGetter(
        classDescriptor: ClassDescriptor,
        field: PropertyDescriptor,
        annotation: AnnotationDescriptor
    ): SimpleFunctionDescriptor? {
        val prefix = if (field.type.isPrimitiveBoolean()) "is" else "get"
        val functionName = Name.identifier(prefix + toPreparedBase(field.name.identifier))
        return createFunction(
            classDescriptor,
            functionName,
            emptyList(),
            field.returnType,
            visibility = getVisibility(annotation)
        )
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <E, R> Collection<E>.collectWithNotNull(f: (E) -> R?): List<Pair<E, R>> =
        map { it to f(it) }.filter { it.second != null } as List<Pair<E, R>>

    private fun KotlinType.isPrimitiveBoolean(): Boolean = this is SimpleTypeMarker && isBoolean()  //todo

}
