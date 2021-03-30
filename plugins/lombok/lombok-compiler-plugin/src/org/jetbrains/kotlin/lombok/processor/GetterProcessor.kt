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
import org.jetbrains.kotlin.lombok.utils.collectWithNotNull
import org.jetbrains.kotlin.lombok.utils.createFunction
import org.jetbrains.kotlin.lombok.utils.getJavaFields
import org.jetbrains.kotlin.lombok.utils.toPreparedBase
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.typeUtil.isBoolean

class GetterProcessor(private val config: LombokConfig) : Processor {

    private val noIsPrefix by lazy { config.getBooleanOrDefault("lombok.getter.noIsPrefix") }

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
        val accessors = Accessors.getIfAnnotated(field, config) ?: globalAccessors

        val functionName =
            if (accessors.fluent) {
                field.name.identifier
            } else {
                val prefix = if (field.type.isPrimitiveBoolean() && !noIsPrefix) "is" else "get"
                prefix + toPreparedBase(field.name.identifier)
            }
        return classDescriptor.createFunction(
            Name.identifier(functionName),
            emptyList(),
            field.returnType,
            visibility = getter.visibility
        )
    }

    private fun KotlinType.isPrimitiveBoolean(): Boolean = this is SimpleTypeMarker && isBoolean()  //todo

}
