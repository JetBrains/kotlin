/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.commonizer.konan.NativeSensitiveManifestData
import org.jetbrains.kotlin.library.SerializedMetadata
import java.io.File

internal fun buildResultsConsumer(init: ResultsConsumerBuilder.() -> Unit): ResultsConsumer {
    return ResultsConsumerBuilder().apply(init).build()
}

interface ResultsConsumer {
    enum class Status { NOTHING_TO_DO, DONE }

    sealed class ModuleResult {
        abstract val libraryName: String

        class Missing(val originalLocation: File) : ModuleResult() {
            override val libraryName: String get() = originalLocation.name
        }

        class Commonized(
            override val libraryName: String, val metadata: SerializedMetadata, val manifest: NativeSensitiveManifestData
        ) : ModuleResult()
    }

    /**
     * Consume a single [ModuleResult] for the specified [CommonizerTarget].
     */
    fun consume(target: CommonizerTarget, moduleResult: ModuleResult) = Unit

    /**
     * Mark the specified [CommonizerTarget] as fully consumed.
     * It's forbidden to make subsequent [consume] calls for fully consumed targets.
     */
    fun targetConsumed(target: CommonizerTarget) = Unit

    /**
     * Notify that all results have been consumed.
     * It's forbidden to make any subsequent [consume] and [targetConsumed] calls after this call.
     */
    fun allConsumed(status: Status) = Unit
}

internal class ResultsConsumerBuilder {
    private var resultsConsumer: ResultsConsumer? = null

    infix fun add(consumer: ResultsConsumer) {
        val resultsConsumer = this.resultsConsumer
        if (resultsConsumer == null) {
            this.resultsConsumer = consumer
            return
        }
        this.resultsConsumer = resultsConsumer + consumer
    }

    fun build(): ResultsConsumer {
        return resultsConsumer ?: object : ResultsConsumer {}
    }
}

operator fun ResultsConsumer.plus(other: ResultsConsumer?): ResultsConsumer {
    if (other == null) return this
    if (this is CompositeResultsConsumer) {
        return CompositeResultsConsumer(consumers + other)
    }
    return CompositeResultsConsumer(listOf(this, other))
}

private class CompositeResultsConsumer(val consumers: List<ResultsConsumer>) : ResultsConsumer {
    override fun consume(target: CommonizerTarget, moduleResult: ResultsConsumer.ModuleResult) {
        consumers.forEach { consumer ->
            consumer.consume(target, moduleResult)
        }
    }

    override fun targetConsumed(target: CommonizerTarget) {
        consumers.forEach { consumer ->
            consumer.targetConsumed(target)
        }
    }

    override fun allConsumed(status: ResultsConsumer.Status) {
        consumers.forEach { consumer ->
            consumer.allConsumed(status)
        }
    }
}
