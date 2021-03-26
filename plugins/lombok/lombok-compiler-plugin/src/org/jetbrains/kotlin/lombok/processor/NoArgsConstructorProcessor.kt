/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.lombok.config.NoArgsConstructor
import org.jetbrains.kotlin.lombok.utils.createConstructor
import org.jetbrains.kotlin.lombok.utils.createFunction
import org.jetbrains.kotlin.name.Name

class NoArgsConstructorProcessor : Processor {

    override fun contribute(classDescriptor: ClassDescriptor, jClass: JavaClassImpl): Parts {
        val result = NoArgsConstructor.getOrNull(classDescriptor)?.let { annotation ->
            if (annotation.staticName == null) {
                val constructor = classDescriptor.createConstructor(
                    valueParameters = emptyList(),
                    visibility = annotation.visibility
                )
                Parts(constructors = listOfNotNull(constructor))
            } else {
                val function = classDescriptor.createFunction(
                    Name.identifier(annotation.staticName),
                    emptyList(),
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
