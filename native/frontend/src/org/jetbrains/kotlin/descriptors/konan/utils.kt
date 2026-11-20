/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.name.Name

@K1Deprecation
private val NATIVE_STDLIB_MODULE_NAME: Name = Name.special("<$KONAN_STDLIB_NAME>")

@K1Deprecation
fun ModuleDescriptor.isNativeStdlib(): Boolean = name == NATIVE_STDLIB_MODULE_NAME

/**
 * @return naturally-ordered list of all parameters available inside the function body.
 */
@K1Deprecation
val CallableDescriptor.allParameters: List<ParameterDescriptor>
    get() = if (this is ConstructorDescriptor) {
        listOf(this.constructedClass.thisAsReceiverParameter) + explicitParameters
    } else {
        explicitParameters
    }

/**
 * @return naturally-ordered list of the parameters that can have values specified at call site.
 */
@K1Deprecation
val CallableDescriptor.explicitParameters: List<ParameterDescriptor>
    get() {
        val result = ArrayList<ParameterDescriptor>(valueParameters.size + 2)

        this.dispatchReceiverParameter?.let {
            result.add(it)
        }

        this.extensionReceiverParameter?.let {
            result.add(it)
        }

        result.addAll(valueParameters)

        return result
    }
