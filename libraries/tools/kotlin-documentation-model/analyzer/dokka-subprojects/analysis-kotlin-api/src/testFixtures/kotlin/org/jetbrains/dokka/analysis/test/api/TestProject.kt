/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.analysis.TestAnalysisContext
import org.jetbrains.dokka.analysis.test.api.analysis.TestAnalysisServices
import org.jetbrains.dokka.analysis.test.api.analysis.TestProjectAnalyzer
import org.jetbrains.dokka.analysis.test.api.analysis.defaultAnalysisLogger
import org.jetbrains.dokka.analysis.test.api.configuration.BaseTestDokkaConfigurationBuilder
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.util.CollectingDokkaConsoleLogger
import org.jetbrains.dokka.analysis.test.api.util.withTempDirectory
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.utilities.DokkaLogger

/**
 * Declares a capability that a project can have plugins
 */
interface Pluggable {
    /**
     * Add a plugin instance to a project
     */
    fun plugin(
        instance: DokkaPlugin
    )
}

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
     * Returns the list of plugins, which will then be directly passed to [DokkaContext].
     *
     * Unlike [DokkaConfiguration.pluginsClasspath], it does not require a JAR file with a configuration of [java.util.ServiceLoader]
     */
    fun getPluginList(): List<DokkaPlugin>

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
 *
 * @param logger logger to be used for running Dokka and tests. Custom loggers like [CollectingDokkaConsoleLogger]
 *               can be useful in verifying the behavior.
 */
fun TestProject.parse(logger: DokkaLogger = defaultAnalysisLogger): DModule = TestProjectAnalyzer.parse(this, logger)

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
 *
 * @param logger logger to be used for running Dokka and tests. Custom loggers like [CollectingDokkaConsoleLogger]
 *               can be useful in verifying the behavior.
 */
fun TestProject.useServices(
    logger: DokkaLogger = defaultAnalysisLogger,
    block: TestAnalysisServices.(context: TestAnalysisContext) -> Unit
): Unit = TestProjectAnalyzer.useServices(this, logger, block)
