/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.config.With
import org.jetbrains.kotlin.lombok.utils.*
import org.jetbrains.kotlin.name.Name

class WithProcessor : Processor {
    override fun contribute(classDescriptor: ClassDescriptor, jClass: JavaClassImpl): Parts {

        val clWith = With.getOrNull(classDescriptor)

        val functions = classDescriptor
            .getJavaFields()
            .collectWithNotNull { With.getOrNull(it) ?: clWith }
            .mapNotNull { (field, annotation) -> createWith(classDescriptor, field, annotation) }

        return Parts(functions)
    }

    private fun createWith(
        classDescriptor: ClassDescriptor,
        field: PropertyDescriptor,
        with: With
    ): SimpleFunctionDescriptor? {
        if (with.visibility == AccessLevel.NONE) return null

        val functionName = "with" + toPropertyNameCapitalized(field.name.identifier)

        return classDescriptor.createFunction(
            Name.identifier(functionName),
            listOf(ValueParameter(field.name, field.type)),
            classDescriptor.defaultType,
            visibility = with.visibility.toDescriptorVisibility()
        )
    }
}
