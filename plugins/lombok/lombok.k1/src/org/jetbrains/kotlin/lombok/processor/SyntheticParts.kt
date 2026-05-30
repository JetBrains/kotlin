/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor

class SyntheticParts(
    val methods: List<SimpleFunctionDescriptor> = [],
    val staticFunctions: List<SimpleFunctionDescriptor> = [],
    val constructors: List<ClassConstructorDescriptor> = [],
    val classes: List<ClassDescriptor> = [],
) {

    operator fun plus(other: SyntheticParts): SyntheticParts = SyntheticParts(
        methods + other.methods,
        staticFunctions + other.staticFunctions,
        constructors + other.constructors,
        classes + other.classes
    )

    companion object {
        val Empty = SyntheticParts()
    }
}

class SyntheticPartsBuilder {
    private val methods: MutableList<SimpleFunctionDescriptor> = []
    private val staticFunctions: MutableList<SimpleFunctionDescriptor> = []
    private val constructors: MutableList<ClassConstructorDescriptor> = []
    private val classes: MutableList<ClassDescriptor> = []

    fun addMethod(method: SimpleFunctionDescriptor) {
        methods += method
    }

    fun addStaticFunction(function: SimpleFunctionDescriptor) {
        staticFunctions += function
    }

    fun addConstructor(constructor: ClassConstructorDescriptor) {
        constructors += constructor
    }

    fun addClass(clazz: ClassDescriptor) {
        classes += clazz
    }

    fun build(): SyntheticParts = SyntheticParts(methods, staticFunctions, constructors, classes)
}
