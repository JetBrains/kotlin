/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.lombok.config.AllArgsConstructor
import org.jetbrains.kotlin.lombok.utils.ValueParameter
import org.jetbrains.kotlin.lombok.utils.createConstructor
import org.jetbrains.kotlin.lombok.utils.createFunction
import org.jetbrains.kotlin.lombok.utils.getJavaFields
import org.jetbrains.kotlin.name.Name

class AllArgsConstructorProcessor : Processor {

    override fun contribute(classDescriptor: ClassDescriptor, jClass: JavaClassImpl): Parts {

        val valueParameters = classDescriptor.getJavaFields().map { property ->
            ValueParameter(property.name, property.type)
        }

        val result = AllArgsConstructor.getOrNull(classDescriptor)?.let { annotation ->
            if (annotation.staticName == null) {
                val constructor = classDescriptor.createConstructor(
                    valueParameters = valueParameters,
                    visibility = annotation.visibility
                )
                Parts(constructors = listOfNotNull(constructor))
            } else {
                val function = classDescriptor.createFunction(
                    Name.identifier(annotation.staticName),
                    valueParameters,
                    classDescriptor.defaultType,
                    visibility = annotation.visibility,
                    receiver = null
                )
                Parts(staticFunctions = listOf(function))
            }
        }
        return result ?: Parts.Empty
    }

}
