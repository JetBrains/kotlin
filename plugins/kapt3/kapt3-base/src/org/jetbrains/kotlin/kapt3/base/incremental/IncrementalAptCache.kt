/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import java.io.File
import java.io.Serializable

class IncrementalAptCache : Serializable {

    private val aggregatingGenerated: MutableMap<File, String?> = mutableMapOf()
    private val aggregatedTypes: MutableSet<String> = linkedSetOf()
    private val isolatingMapping: MutableMap<File, Pair<String?, File>> = mutableMapOf()
    // Annotations claimed by aggregating annotation processors
    private val aggregatingClaimedAnnotations: MutableSet<String> = mutableSetOf()

    var isIncremental = true
        private set

    fun updateCache(processors: List<IncrementalProcessor>): Boolean {
        val aggregating = mutableListOf<IncrementalProcessor>()
        val isolating = mutableListOf<IncrementalProcessor>()
        val nonIncremental = mutableListOf<IncrementalProcessor>()
        processors.forEach {
            when (it.getRuntimeType()) {
                RuntimeProcType.AGGREGATING -> aggregating.add(it)
                RuntimeProcType.ISOLATING -> isolating.add(it)
                RuntimeProcType.NON_INCREMENTAL -> nonIncremental.add(it)
            }
        }

        if (nonIncremental.isNotEmpty()) {
            invalidateCache()
            return false
        }

        aggregatingGenerated.clear()
        aggregating.forEach {
            it.getGeneratedToSourcesAll().mapValuesTo(aggregatingGenerated) { (_, value) ->
                value?.first
            }
        }

        aggregatingClaimedAnnotations.clear()
        aggregatingClaimedAnnotations.addAll(aggregating.flatMap { it.supportedAnnotationTypes })

        aggregatedTypes.clear()
        aggregatedTypes.addAll(aggregating.flatMap { it.getAggregatedTypes() })

        for (isolatingProcessor in isolating) {
            isolatingProcessor.getGeneratedToSourcesAll().forEach {
                isolatingMapping[it.key] = it.value!!.first to it.value!!.second!!
            }
        }
        return true
    }

    fun getAggregatingClaimedAnnotations(): Set<String> = aggregatingClaimedAnnotations

    /** Returns generated Java sources originating from aggregating APs. */
    fun invalidateAggregating(): Pair<List<File>, List<String>> {
        val dirtyAggregating = aggregatingGenerated.keys.filter { it.isJavaFileOrClass() }
        aggregatingGenerated.forEach { it.key.delete() }
        aggregatingGenerated.clear()

        val dirtyAggregated = ArrayList(aggregatedTypes)
        aggregatedTypes.clear()

        return dirtyAggregating to dirtyAggregated
    }

    /** Returns generated Java sources originating from the specified sources, and generated  by isloating APs. */
    fun invalidateIsolatingGenerated(fromSources: Set<File>): Pair<List<File>, Set<String>> {
        val allInvalidated = mutableListOf<File>()
        val invalidatedClassIds = mutableSetOf<String>()
        var changedSources = fromSources.toSet()

        // We need to do it in a loop because mapping could be: [AGenerated.java -> A.java, AGeneratedGenerated.java -> AGenerated.java]
        while (changedSources.isNotEmpty()) {
            val generated = isolatingMapping.filter { changedSources.contains(it.value.second) }.keys
            generated.forEach {
                if (it.isJavaFileOrClass()) {
                    allInvalidated.add(it)
                    isolatingMapping[it]?.first?.let { invalidatedClassIds.add(it) }
                }

                it.delete()
                isolatingMapping.remove(it)
            }
            changedSources = generated
        }
        return allInvalidated to invalidatedClassIds
    }

    private fun File.isJavaFileOrClass() = extension == "java" || extension == "class"

    private fun invalidateCache() {
        isIncremental = false
        aggregatingGenerated.clear()
        isolatingMapping.clear()
    }
}