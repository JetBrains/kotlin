/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.lombok.config.*
import org.jetbrains.kotlin.lombok.utils.*
import org.jetbrains.kotlin.name.Name

class GetterProcessor(private val config: LombokConfig) : Processor {

    override fun contribute(classDescriptor: ClassDescriptor, jClass: JavaClassImpl): Parts {
        val globalAccessors = Accessors.get(classDescriptor, config)
        val clGetter =
            Getter.getOrNull(classDescriptor)
                ?: Data.getOrNull(classDescriptor)?.asGetter()
                ?: Value.getOrNull(classDescriptor)?.asGetter()

        val functions = classDescriptor
            .getJavaFields()
            .collectWithNotNull { Getter.getOrNull(it) ?: clGetter }
            .mapNotNull { (field, annotation) -> createGetter(classDescriptor, field, annotation, globalAccessors) }

        return Parts(functions)
    }

    private fun createGetter(
        classDescriptor: ClassDescriptor,
        field: PropertyDescriptor,
        getter: Getter,
        globalAccessors: Accessors
    ): SimpleFunctionDescriptor? {
        if (getter.visibility == AccessLevel.NONE) return null

        val accessors = Accessors.getIfAnnotated(field, config) ?: globalAccessors
        return field.toPropertyName(accessors)?.let { propertyName ->
            val functionName =
                if (accessors.fluent) {
                    propertyName
                } else {
                    val prefix = if (field.type.isPrimitiveBoolean() && !accessors.noIsPrefix) AccessorNames.IS else AccessorNames.GET
                    prefix + propertyName.capitalize()
                }
            classDescriptor.createFunction(
                Name.identifier(functionName),
                emptyList(),
                field.returnType,
                visibility = getter.visibility.toDescriptorVisibility()
            )
        }
    }

}
