/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.lombok.config.LombokAnnotations.ConstructorAnnotation
import org.jetbrains.kotlin.lombok.utils.LombokValueParameter
import org.jetbrains.kotlin.lombok.utils.createFunction
import org.jetbrains.kotlin.lombok.utils.createJavaConstructor
import org.jetbrains.kotlin.name.Name

abstract class AbstractConstructorProcessor<A : ConstructorAnnotation> : Processor {

    override fun contribute(classDescriptor: ClassDescriptor, partsBuilder: SyntheticPartsBuilder, c: LazyJavaResolverContext) {
        getAnnotation(classDescriptor)?.let { annotation ->
            val valueParameters = getPropertiesForParameters(classDescriptor).map { property ->
                LombokValueParameter(property.name, property.type)
            }
            if (annotation.staticName == null) {
                val constructor = classDescriptor.createJavaConstructor(
                    valueParameters = valueParameters,
                    visibility = annotation.visibility
                )
                partsBuilder.addConstructor(constructor)
            } else {
                val function = classDescriptor.createFunction(
                    Name.identifier(annotation.staticName!!),
                    valueParameters,
                    classDescriptor.defaultType,
                    typeParameters = classDescriptor.declaredTypeParameters,
                    visibility = annotation.visibility,
                    receiver = null
                )
                partsBuilder.addStaticFunction(function)
            }
        }
    }

    protected abstract fun getAnnotation(classDescriptor: ClassDescriptor): A?

    protected abstract fun getPropertiesForParameters(classDescriptor: ClassDescriptor): List<PropertyDescriptor>
}
