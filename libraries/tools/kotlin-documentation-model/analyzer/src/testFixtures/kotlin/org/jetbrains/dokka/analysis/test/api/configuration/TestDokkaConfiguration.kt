/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.configuration

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.test.api.TestProject

/**
 * Configuration options for [TestProject].
 *
 * Represents a trimmed-down version of [DokkaConfiguration] that only
 * exposes properties that can be used by Dokka's analysis implementations.
 */
data class TestDokkaConfiguration(

    /**
     * Name of this [TestProject].
     *
     * @see DokkaConfiguration.moduleName
     */
    val moduleName: String,

    /**
     * References Markdown files that contain documentation for this module and packages.
     *
     * Contains paths relative to the root of [TestProject], so it must begin with `/`.
     *
     * Example: `/docs/module.md`
     *
     * @see DokkaConfiguration.includes
     * @see https://kotlinlang.org/docs/dokka-module-and-package-docs.html
     */
    val includes: Set<String> = emptySet(),

    /**
     * A number of source directories and their configuration options that
     * make up this project.
     *
     * A multiplatform Kotlin project will typically have multiple source sets
     * for the supported platforms, whereas a single Kotlin/JVM project will have only one.
     *
     * @see TestDokkaSourceSet
     */
    val sourceSets: Set<TestDokkaSourceSet>


) {
    override fun toString(): String {
        return "TestDokkaConfiguration(moduleName='$moduleName', includes=$includes, sourceSets=$sourceSets)"
    }
}

/**
 * Configuration options for a collection of source files for a specific platform.
 *
 * Represents a trimmed-down version of [DokkaConfiguration.DokkaSourceSet] that only
 * exposes properties that can be used by Dokka's analysis implementations.
 *
 * @see https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets
 */
data class TestDokkaSourceSet (

    /**
     * @see DokkaConfiguration.DokkaSourceSet.analysisPlatform
     */
    val analysisPlatform: Platform,

    /**
     * Display name of the source set, used both internally and externally
     *
     * @see DokkaConfiguration.DokkaSourceSet.displayName
     */
    val displayName: String,

    /**
     * A unique identifier of this source set in the scope of the project.
     *
     * It must be unique even if two source sets have the same name,
     * the same platform or the same configuration.
     *
     * @see DokkaConfiguration.DokkaSourceSet.sourceSetID
     */
    val sourceSetID: DokkaSourceSetID,

    /**
     * A set of source set ids that this source set depends on.
     *
     * @see DokkaConfiguration.DokkaSourceSet.dependentSourceSets
     */
    val dependentSourceSets: Set<DokkaSourceSetID>,

    /**
     * A set of directories that contain the source files for this source set.
     *
     * A source set typically has only one source root directory, but there can be additional
     * ones if the project has generated sources that reside separately, or if the `@sample`
     * tag is used, and the samples reside in a different directory.
     *
     * Contains paths relative to the root of [TestProject], so it must begin with `/`.
     *
     * @see DokkaConfiguration.DokkaSourceSet.sourceRoots
     */
    val sourceRoots: Set<String>,

    /**
     * A set of JARs to be used for analyzing sources.
     *
     * If this [TestProject] exposes types from external libraries, Dokka needs to know
     * about these libraries so that it can generate correct signatures and documentation.
     *
     * Contains absolute paths, can be any file, even if it has nothing to do with unit
     * tests or test project.
     *
     * @see DokkaConfiguration.DokkaSourceSet.classpath
     */
    val classpath: Set<String>,

    /**
     * References Markdown files that contain documentation for this module and packages.
     *
     * Contains paths relative to the root of [TestProject], so it must begin with `/`.
     *
     * Example: `/docs/module.md`
     *
     * @see DokkaConfiguration.DokkaSourceSet.includes
     * @see https://kotlinlang.org/docs/dokka-module-and-package-docs.html
     */
    val includes: Set<String> = emptySet(),

    /**
     * A set of Kotlin files with functions that show how to use certain API.
     *
     * Contains paths relative to the root of [TestProject], so it must begin with `/`.
     *
     * Example: `/samples/collectionSamples.kt`
     *
     * @see DokkaConfiguration.DokkaSourceSet.samples
     */
    val samples: Set<String> = emptySet(),

    /**
     * Compatibility mode for Kotlin language version X.Y.
     *
     * Example: `1.9`
     *
     * @see https://kotlinlang.org/docs/compatibility-modes.html
     */
    val languageVersion: String? = null,

    /**
     * Compatibility mode for Kotlin API version X.Y.
     *
     * Example: `1.9`
     *
     * @see https://kotlinlang.org/docs/compatibility-modes.html
     */
    val apiVersion: String? = null,


) {
    override fun toString(): String {
        return "TestDokkaSourceSet(" +
                "analysisPlatform=$analysisPlatform, " +
                "sourceSetID=$sourceSetID, " +
                "dependentSourceSets=$dependentSourceSets, " +
                "sourceRoots=$sourceRoots, " +
                "classpath=$classpath, " +
                "includes=$includes, " +
                "samples=$samples, " +
                "languageVersion=$languageVersion, " +
                "apiVersion=$apiVersion" +
                ")"
    }
}
