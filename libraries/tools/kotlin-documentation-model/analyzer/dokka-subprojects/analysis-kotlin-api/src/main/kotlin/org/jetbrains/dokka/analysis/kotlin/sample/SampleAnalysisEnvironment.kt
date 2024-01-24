/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.sample

import org.jetbrains.dokka.DokkaConfiguration

/**
 * Fully-configured and ready-to-use sample analysis environment.
 *
 * It's best to limit the scope of use and lifetime of this environment as it takes up
 * additional resources which could be freed once the samples have been analyzed.
 * Therefore, it's best to use it through the [SampleAnalysisEnvironmentCreator.use] lambda.
 *
 * For example, if you need to process all samples in an arbitrary project, it's best to do it
 * in one iteration and at the same time, so that the environment is created once and lives for
 * as little is possible, as opposed to creating it again and again for every individual sample.
 */
public interface SampleAnalysisEnvironment {

    /**
     * Resolves a Kotlin sample function by its fully qualified name, and returns its import statements and body.
     *
     * @param sourceSet must be either the source set in which this sample function resides, or the source set
     *                  for which [DokkaConfiguration#samples] or [DokkaConfiguration#sourceRoots]
     *                  have been configured with the sample's sources.
     * @param fullyQualifiedLink fully qualified path to the sample function, including all middle packages
     *                           and the name of the function. Only links to Kotlin functions are valid,
     *                           which can reside within a class. The package must be the same as the package
     *                           declared in the sample file. The function must be resolvable by Dokka,
     *                           meaning it must reside either in the main sources of the project or its
     *                           sources must be included in [DokkaConfiguration#samples] or
     *                           [DokkaConfiguration#sourceRoots]. Example: `com.example.pckg.topLevelKotlinFunction`
     *
     * @return a sample code snippet which includes import statements and the function body,
     *         or null if the link could not be resolved (examine the logs to find out the reason).
     */
    public fun resolveSample(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        fullyQualifiedLink: String
    ): SampleSnippet?
}
