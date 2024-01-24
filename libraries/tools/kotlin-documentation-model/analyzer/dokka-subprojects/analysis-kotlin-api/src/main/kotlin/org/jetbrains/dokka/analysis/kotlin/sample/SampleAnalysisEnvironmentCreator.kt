/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.sample

import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin

/**
 * Entry point to analyzing Kotlin samples.
 *
 * Can be acquired via [KotlinAnalysisPlugin.sampleAnalysisEnvironmentCreator].
 */
public interface SampleAnalysisEnvironmentCreator {

    /**
     * Creates and configures the sample analysis environment for a limited-time use.
     *
     * Configuring sample analysis environment is a rather expensive operation that takes up additional
     * resources since Dokka needs to configure and analyze source roots additional to the main ones.
     * It's best to limit the scope of use and the lifetime of the created environment
     * so that the resources could be freed as soon as possible.
     *
     * No specific cleanup is required by the caller - everything is taken care of automatically
     * as soon as you exit the [block] block.
     *
     * Usage example:
     * ```kotlin
     * // create a short-lived environment and resolve all the needed samples
     * val sample = sampleAnalysisEnvironmentCreator.use {
     *     resolveSample(sampleSourceSet, "org.jetbrains.dokka.sample.functionName")
     * }
     * // process the samples
     * // ...
     * ```
     */
    public fun <T> use(block: SampleAnalysisEnvironment.() -> T): T
}
