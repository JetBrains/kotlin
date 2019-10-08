/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.execution

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinExecution
import org.jetbrains.kotlin.gradle.plugin.KotlinExecution.ExecutionSource

/**
 * An execution source that aggregates other [executionSources].
 */
interface KotlinAggregateExecutionSource<AggregatedSourceType : ExecutionSource> : ExecutionSource {
    val executionSources: Iterable<AggregatedSourceType>
}

interface KotlinAggregatingExecution<
        AggregatedSourceType : ExecutionSource,
        AggregatingSourceType : KotlinAggregateExecutionSource<AggregatedSourceType>,
        AggregatedExecutionType : KotlinExecution<AggregatedSourceType>
        > :
    KotlinExecution<AggregatingSourceType> {
    /**
     * Configures all of the executions aggregated by this execution. If some of the executions are not yet created up to this point,
     * [configure] will be called on them later, once they are created.
     */
    fun configureAllExecutions(configure: AggregatedExecutionType.() -> Unit): Unit

    /**
     * Returns the aggregated executions that are already configured up to this moment.
     * Some test runs may be missing from the results if they are not yet configured.
     */
    fun getConfiguredExecutions(): Iterable<AggregatedExecutionType>

    fun configureAllExecutions(configureClosure: Closure<*>) = configureAllExecutions {
        ConfigureUtil.configureSelf(
            configureClosure,
            this
        )
    }
}