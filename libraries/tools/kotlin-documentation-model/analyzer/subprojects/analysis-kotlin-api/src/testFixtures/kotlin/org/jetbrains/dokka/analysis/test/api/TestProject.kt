/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.analysis.TestAnalysisContext
import org.jetbrains.dokka.analysis.test.api.analysis.TestAnalysisServices
import org.jetbrains.dokka.analysis.test.api.analysis.TestProjectAnalyzer
import org.jetbrains.dokka.analysis.test.api.configuration.BaseTestDokkaConfigurationBuilder
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.util.withTempDirectory
import org.jetbrains.dokka.model.DModule

/**
 * Represents a virtual test project (as if it's user-defined) that will be used to run Dokka.
 *
 * A test project consists of some Dokka configuration (represented as [TestDokkaConfiguration])
 * and some project-specific data like source code and markdown files (represented as [TestData]).
 *
 * See [kotlinJvmTestProject], [javaTestProject] and [mixedJvmTestProject] for convenient ways
 * of bootstrapping test projects.
 *
 * See [parse] and [useServices] functions to learn how to run Dokka with this project as input.
 */
interface TestProject {

    /**
     * Verifies that this project is valid from the user's and Dokka's perspectives.
     * Exists to save time with debugging difficult to catch mistakes, such as copy-pasted
     * test data that is not applicable to this project.
     *
     * Must throw an exception if there's misconfiguration, incorrect / corrupted test data
     * or API misuse.
     *
     * Verification is performed before running Dokka on this project.
     */
    fun verify()

    /**
     * Returns the configuration of this project, which will then be mapped to [DokkaConfiguration].
     *
     * This is typically constructed using [BaseTestDokkaConfigurationBuilder].
     */
    fun getConfiguration(): TestDokkaConfiguration

    /**
     * Returns this project's test data - a collection of source code files, markdown files
     * and whatever else that can be usually found in a user-defined project.
     */
    fun getTestData(): TestData
}

/**
 * Runs Dokka on the given [TestProject] and returns the generated documentable model.
 *
 * Can be used to verify the resulting documentable model, to check that
 * everything was parsed and converted correctly.
 *
 * Usage example:
 * ```kotlin
 * val testProject = kotlinJvmTestProject {
 *     ...
 * }
 *
 * val module: DModule = testProject.parse()
 * ```
 */
fun TestProject.parse(): DModule = TestProjectAnalyzer.parse(this)

/**
 * Runs Dokka on the given [TestProject] and provides not only the resulting documentable model,
 * but analysis context and configuration as well, which gives you the ability to call public
 * analysis services.
 *
 * Usage example:
 *
 * ```kotlin
 * val testProject = kotlinJvmTestProject {
 *     ...
 * }
 *
 * testProject.useServices { context ->
 *     val pckg: DPackage = context.module.packages.single()
 *
 *     // use `moduleAndPackageDocumentationReader` service to get documentation of a package
 *     val allPackageDocs: SourceSetDependent<DocumentationNode> = moduleAndPackageDocumentationReader.read(pckg)
 * }
 * ```
 */
fun TestProject.useServices(block: TestAnalysisServices.(context: TestAnalysisContext) -> Unit) {
    withTempDirectory { tempDirectory ->
        val (services, context) = TestProjectAnalyzer.analyze(this, tempDirectory)
        services.block(context)
    }
}
