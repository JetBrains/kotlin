/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.configuration

import org.jetbrains.dokka.analysis.test.api.TestProject
import org.jetbrains.dokka.analysis.test.api.util.getResourceAbsolutePath
import org.jetbrains.dokka.analysis.test.api.util.AnalysisTestDslMarker

/**
 * A builder for [TestDokkaConfiguration] that contains base options.
 *
 * Implementations can override the properties to modify the resulting test
 * data or prohibit setting certain properties.
 */
@AnalysisTestDslMarker
abstract class BaseTestDokkaConfigurationBuilder {

    /**
     * @see TestDokkaConfiguration.moduleName
     */
    abstract var moduleName: String


    /**
     * @see TestDokkaConfiguration.includes
     */
    open var includes: Set<String> = emptySet()

    /**
     * Verifies that this source set configuration is valid. For example,
     * it should check that all file paths are set in the expected format.
     *
     * Must be invoked manually as part of [TestProject.verify].
     */
    open fun verify() {
        includes.forEach {
            verifyFilePathStartsWithSlash("includes", it)
            verifyFileExtension("includes", it, ".md")
        }
    }

    abstract fun build(): TestDokkaConfiguration

    override fun toString(): String {
        return "BaseTestDokkaConfigurationBuilder(moduleName='$moduleName', includes=$includes)"
    }
}

/**
 * A builder for [TestDokkaSourceSet] that contains base options.
 *
 * Implementations can override the properties to modify the resulting test
 * data or prohibit setting certain properties.
 */
@AnalysisTestDslMarker
abstract class BaseTestDokkaSourceSetBuilder {

    /**
     * Directories **additional** to the default source roots.
     *
     * @see TestDokkaSourceSet.sourceRoots
     */
    open var additionalSourceRoots: Set<String> = emptySet()

    /**
     * JARs **additional** to the default classpath.
     *
     * You can put test JARs inside `src/resources`, and then get it via [getResourceAbsolutePath].
     *
     * @see TestDokkaSourceSet.classpath
     */
    open var additionalClasspath: Set<String> = emptySet()

    /**
     * @see TestDokkaSourceSet.includes
     */
    open var includes: Set<String> = emptySet()

    /**
     * @see TestDokkaSourceSet.samples
     */
    open var samples: Set<String> = emptySet()

    /**
     * @see TestDokkaSourceSet.languageVersion
     */
    open var languageVersion: String? = null

    /**
     * @see TestDokkaSourceSet.apiVersion
     */
    open var apiVersion: String? = null

    /**
     * Verifies that this source set configuration is valid. For example,
     * it should check that all file paths are set in the expected format.
     *
     * Must be invoked manually during the verification of the
     * higher-level [BaseTestDokkaConfigurationBuilder.verify].
     */
    open fun verify() {
        additionalSourceRoots.forEach {
            verifyFilePathStartsWithSlash("additionalSourceRoots", it)
        }
        additionalClasspath.forEach {
            // this check can be extended to accept .klib, .class or other files
            // as the need for it arises, as long as Dokka supports it
            verifyFileExtension("additionalClasspath", it, ".jar")
        }
        includes.forEach {
            verifyFilePathStartsWithSlash("includes", it)
            verifyFileExtension("includes", it, ".md")
        }
        samples.forEach {
            verifyFilePathStartsWithSlash("samples", it)
            verifyFileExtension("samples", it, ".kt")
        }
    }

    abstract fun build(): TestDokkaSourceSet

    override fun toString(): String {
        return "BaseTestDokkaSourceSetBuilder(" +
                "additionalSourceRoots=$additionalSourceRoots, " +
                "additionalClasspath=$additionalClasspath, " +
                "includes=$includes, " +
                "samples=$samples, " +
                "languageVersion=$languageVersion, " +
                "apiVersion=$apiVersion" +
                ")"
    }
}

internal fun verifyFilePathStartsWithSlash(propertyName: String, path: String) {
    require(path.startsWith("/")) {
        "Property $propertyName must contain paths relative to the root of the project. " +
                "Please, prefix it with \"/\" for readability and consistency."
    }
}

internal fun verifyFileExtension(propertyName: String, filePath: String, expectedExtension: String) {
    require(filePath.endsWith(expectedExtension)) {
        "Property $propertyName only accepts files with \"$expectedExtension\" extension. " +
                "Got: \"${filePath.substringAfterLast("/")}\"."
    }
}
