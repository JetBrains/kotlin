/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
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
            NoArgsConstructorProcessor(),
            AllArgsConstructorProcessor(),
            RequiredArgsConstructorProcessor()
        )

    private val partsCache: MutableMap<ClassDescriptor, Parts> = WeakHashMap()

    override fun getMethodNames(thisDescriptor: ClassDescriptor): List<Name> =
        getSyntheticParts(thisDescriptor).methods.map { it.name }

    override fun generateMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        val methods = getSyntheticParts(thisDescriptor).methods.filter { it.name == name }
        addNonExistent(result, methods)
    }

    override fun getStaticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
        getSyntheticParts(thisDescriptor).staticFunctions.map { it.name }

    override fun generateStaticFunctions(thisDescriptor: ClassDescriptor, name: Name, result: MutableCollection<SimpleFunctionDescriptor>) {
        val functions = getSyntheticParts(thisDescriptor).staticFunctions.filter { it.name == name }
        addNonExistent(result, functions)
    }

    override fun generateConstructors(thisDescriptor: ClassDescriptor, result: MutableList<ClassConstructorDescriptor>) {
        val constructors = getSyntheticParts(thisDescriptor).constructors
        addNonExistent(result, constructors)
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

    private fun <T : FunctionDescriptor> addNonExistent(result: MutableCollection<T>, toAdd: List<T>) {
        if (toAdd.isEmpty()) return

        val index = mutableMapOf<Name, List<T>>()

        fun addToIndex(f: T) {
            index.merge(f.name, listOf(f)) { a, b -> a + b}
        }

        result.forEach(::addToIndex)

        toAdd.forEach { f ->
            val existing = index.getOrDefault(f.name, emptyList())
            if (existing.none { sameSignature (it, f) } ) {
                result += f
                addToIndex(f)
            }
        }
    }


    companion object {

        /**
         * Lombok treat functions as having the same signature by arguments count only
         */
        private fun sameSignature(a: FunctionDescriptor, b: FunctionDescriptor): Boolean {
            val aVararg = a.valueParameters.any { it.varargElementType != null }
            val bVararg = b.valueParameters.any { it.varargElementType != null }
            return aVararg && bVararg ||
                    aVararg && b.valueParameters.size >= (a.valueParameters.size - 1) ||
                    bVararg && a.valueParameters.size >= (b.valueParameters.size - 1) ||
                    a.valueParameters.size == b.valueParameters.size
        }
    }
}
