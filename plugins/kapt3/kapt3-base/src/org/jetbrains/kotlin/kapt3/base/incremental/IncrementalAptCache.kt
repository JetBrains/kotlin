/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import java.io.File
import java.io.Serializable

class IncrementalAptCache : Serializable {

    private val aggregatingGenerated: MutableSet<File> = mutableSetOf()
    private val isolatingMapping: MutableMap<File, File> = mutableMapOf()
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
        aggregatingGenerated.addAll(aggregating.flatMap { it.getGeneratedToSources().keys })

        aggregatingClaimedAnnotations.clear()
        aggregatingClaimedAnnotations.addAll(aggregating.flatMap { it.supportedAnnotationTypes })

        for (isolatingProcessor in isolating) {
            isolatingProcessor.getGeneratedToSources().forEach {
                isolatingMapping[it.key] = it.value!!
            }
        }
        return true
    }

    fun invalidateAggregatingAndGetAnnotations(): Set<String> {
        aggregatingGenerated.forEach { it.delete() }
        aggregatingGenerated.clear()

        return aggregatingClaimedAnnotations
    }

    fun invalidateIsolatingGenerated(fromSources: Set<File>) {
        var changedSources = fromSources.toSet()

        // We need to do it in a loop because mapping could be: [AGenerated.java -> A.java, AGeneratedGenerated.java -> AGenerated.java]
        while(changedSources.isNotEmpty()) {
            val generated = isolatingMapping.filter { changedSources.contains(it.value) }.keys
            generated.forEach {
                it.delete()
                isolatingMapping.remove(it)
            }
            changedSources = generated
        }
    }

    private fun invalidateCache() {
        isIncremental = false
        aggregatingGenerated.clear()
        isolatingMapping.clear()
    }
}