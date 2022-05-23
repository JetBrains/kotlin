/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.lombok.config.LombokAnnotations.AllArgsConstructor
import org.jetbrains.kotlin.lombok.config.LombokAnnotations.Value
import org.jetbrains.kotlin.lombok.utils.getJavaFields

class AllArgsConstructorProcessor : AbstractConstructorProcessor<AllArgsConstructor>() {

    override fun getAnnotation(classDescriptor: ClassDescriptor): AllArgsConstructor? =
        AllArgsConstructor.getOrNull(classDescriptor) ?: Value.getOrNull(classDescriptor)?.asAllArgsConstructor()

    override fun getPropertiesForParameters(classDescriptor: ClassDescriptor): List<PropertyDescriptor> =
        classDescriptor.getJavaFields()

}
