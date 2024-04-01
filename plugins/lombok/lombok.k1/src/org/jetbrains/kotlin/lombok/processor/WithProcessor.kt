/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.config.LombokAnnotations.With
import org.jetbrains.kotlin.lombok.config.toDescriptorVisibility
import org.jetbrains.kotlin.lombok.utils.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.typeUtil.isBoolean

class WithProcessor : Processor {
    override fun contribute(classDescriptor: ClassDescriptor, partsBuilder: SyntheticPartsBuilder, c: LazyJavaResolverContext) {
        val clWith = With.getOrNull(classDescriptor)

        classDescriptor
            .getJavaFields()
            .collectWithNotNull { With.getOrNull(it) ?: clWith }
            .mapNotNull { (field, annotation) -> createWith(classDescriptor, field, annotation) }
            .forEach(partsBuilder::addMethod)
    }

    private fun createWith(
        classDescriptor: ClassDescriptor,
        field: PropertyDescriptor,
        with: With
    ): SimpleFunctionDescriptor? {
        if (with.visibility == AccessLevel.NONE) return null

        val rawPropertyName = field.name.identifier
        val propertyName = if (field.type.isBoolean() && rawPropertyName.startsWith("is")) {
            rawPropertyName.removePrefix("is")
        } else {
            rawPropertyName
        }

        val functionName = "with" + toPropertyNameCapitalized(propertyName)

        return classDescriptor.createFunction(
            Name.identifier(functionName),
            listOf(LombokValueParameter(field.name, field.type)),
            classDescriptor.defaultType,
            visibility = with.visibility.toDescriptorVisibility()
        )
    }
}
