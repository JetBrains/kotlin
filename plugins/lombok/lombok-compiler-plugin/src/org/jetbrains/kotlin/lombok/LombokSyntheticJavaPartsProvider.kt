/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.lombok.config.LombokConfig
import org.jetbrains.kotlin.lombok.processor.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.SyntheticJavaPartsProvider
import java.util.*

class LombokSyntheticJavaPartsProvider(private val config: LombokConfig) : SyntheticJavaPartsProvider {

    private val processors = initProcessors()

    private fun initProcessors(): List<Processor> =
        listOf(
            GetterProcessor(config),
            SetterProcessor(config),
            WithProcessor(config),
        )

    private val partsCache: MutableMap<ClassDescriptor, Parts> = WeakHashMap()

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
        getSyntheticParts(thisDescriptor).methods.map { it.name }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        val methods = getSyntheticParts(thisDescriptor).methods.filter { it.name == name }
        result.addAll(methods)
    }

    private fun extractClass(descriptor: ClassDescriptor): JavaClassImpl? =
        (descriptor as? LazyJavaClassDescriptor)?.jClass as? JavaClassImpl

    private fun getSyntheticParts(descriptor: ClassDescriptor): Parts =
        extractClass(descriptor)?.let { jClass ->
            partsCache.getOrPut(descriptor) {
                computeSyntheticParts(descriptor, jClass)
            }
        } ?: Parts.Empty

    private fun computeSyntheticParts(descriptor: ClassDescriptor, jClass: JavaClassImpl): Parts =
        processors.map { it.contribute(descriptor, jClass) }.reduce { a, b -> a + b }


}
