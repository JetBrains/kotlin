/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor

class SyntheticParts(
    val methods: List<SimpleFunctionDescriptor> = emptyList(),
    val staticFunctions: List<SimpleFunctionDescriptor> = emptyList(),
    val constructors: List<ClassConstructorDescriptor> = emptyList()
) {

    operator fun plus(other: SyntheticParts): SyntheticParts = SyntheticParts(
        methods + other.methods,
        staticFunctions + other.staticFunctions,
        constructors + other.constructors
    )

    companion object {
        val Empty = SyntheticParts()
    }
}

class SyntheticPartsBuilder {
    private val methods = mutableListOf<SimpleFunctionDescriptor>()
    private val staticFunctions = mutableListOf<SimpleFunctionDescriptor>()
    private val constructors = mutableListOf<ClassConstructorDescriptor>()

    fun addMethod(method: SimpleFunctionDescriptor) {
        methods += method
    }

    fun addStaticFunction(function: SimpleFunctionDescriptor) {
        staticFunctions += function
    }

    fun addConstructor(constructor: ClassConstructorDescriptor) {
        constructors += constructor
    }

    fun build(): SyntheticParts = SyntheticParts(methods, staticFunctions, constructors)
}
