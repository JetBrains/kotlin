/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.lombok.config.LombokAnnotations
import org.jetbrains.kotlin.lombok.config.LombokConfig

class ValueFieldModifier(val config: LombokConfig) {
    fun modifyField(thisDescriptor: ClassDescriptor, propertyDescriptor: PropertyDescriptorImpl): PropertyDescriptorImpl? {
        // Explicit visibility
        if (propertyDescriptor.visibility != JavaDescriptorVisibilities.PACKAGE_VISIBILITY) return null
        if (LombokAnnotations.Value.getOrNull(thisDescriptor) == null) return null
        return propertyDescriptor.newCopyBuilder().apply {
            setVisibility(DescriptorVisibilities.PRIVATE)
        }.build() as? PropertyDescriptorImpl
    }
}
