/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor

data class Parts(
    val methods: List<SimpleFunctionDescriptor> = emptyList(),
    val constructors: List<ClassConstructorDescriptor> = emptyList()
) {

    operator fun plus(other: Parts): Parts = Parts(methods + other.methods, constructors + other.constructors)

    companion object {
        val Empty = Parts()
    }
}
