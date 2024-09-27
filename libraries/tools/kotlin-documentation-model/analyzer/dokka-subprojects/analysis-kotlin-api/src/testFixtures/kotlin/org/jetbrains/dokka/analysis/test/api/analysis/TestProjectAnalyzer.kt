/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(InternalDokkaApi::class)

package org.jetbrains.dokka.analysis.test.api.analysis

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin
import org.jetbrains.dokka.analysis.test.api.TestDataFile
import org.jetbrains.dokka.analysis.test.api.TestProject
import org.jetbrains.dokka.analysis.test.api.analysis.TestProjectAnalyzer.parse
import org.jetbrains.dokka.analysis.test.api.configuration.toDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.util.withTempDirectory
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.documentation.DefaultDocumentableMerger
import org.jetbrains.dokka.transformers.documentation.DocumentableMerger
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import java.io.File

/**
 * The default logger used for running Dokka and analyzing projects.
 */
val defaultAnalysisLogger = DokkaConsoleLogger(minLevel = LoggingLevel.DEBUG)

/**
 * Analyzer of the test projects, it is essentially a very simple Dokka runner.
 *
 * Takes all virtual files of the given [TestProject], creates the real files for
 * them in a temporary directory, and then runs Dokka with this temporary directory
 * as the input user project. This allows us to simulate Dokka's behavior and results
 * on a made-up project as if it were real and run via the CLI runner.
 *
 * Executes only a limited number of steps and uses a small subset of [CoreExtensions]
 * that are necessary to test the analysis logic.
 *
 * Works only with single-module projects, where the source code of this project
 * resides in the root `src` directory. Works with multiple source sets and targets,
 * so both simple Kotlin/JVM and more complicated Kotlin Multiplatform project must work.
 */
internal object TestProjectAnalyzer {

    /**
     * A quick way to analyze a [TestProject], for cases when only the documentable
     * model is needed to verify the result.
     *
     * Creates the input test files, runs Dokka and then deletes them right after the documentable
     * model has been created, leaving no trailing files or any other garbage behind.
     *
     * @see [TestProject.parse] for a user-friendly way to call it
     */
    fun parse(testProject: TestProject, logger: DokkaLogger): DModule = withTempDirectory(logger) { tempDirectory ->
        val (_, dokkaContext) = testProject.initialize(outputDirectory = tempDirectory, logger)
        try {
            generateDocumentableModel(dokkaContext, logger)
        } finally {
            cleanup(dokkaContext)
        }
    }

    /**
     * Works in the same way as [parse], but provides not only the resulting documentable model,
     * but analysis context and configuration as well.
     *
     * Creates the input test files, runs Dokka and then deletes them right after the [block]
     * has been executed, leaving no trailing files or any other garbage behind.
     *
     * @see [TestProject.useServices] for a user-friendly way to call it
     */
    fun useServices(
        testProject: TestProject,
        logger: DokkaLogger = defaultAnalysisLogger,
        block: TestAnalysisServices.(context: TestAnalysisContext) -> Unit
    ): Unit = withTempDirectory(logger) { tempDirectory ->
        val (dokkaConfiguration, dokkaContext) = testProject.initialize(outputDirectory = tempDirectory, logger)
        try {
            val analysisServices = createTestAnalysisServices(dokkaContext, logger)
            val testAnalysisContext = TestAnalysisContext(
                context = dokkaContext,
                configuration = dokkaConfiguration,
                module = generateDocumentableModel(dokkaContext, logger)
            )
            analysisServices.block(testAnalysisContext)
        } finally {
            cleanup(dokkaContext)
        }
    }

    /**
     * Prepares this [TestProject] for analysis by creating
     * the test files, setting up context and configuration.
     */
    private fun TestProject.initialize(
        outputDirectory: File,
        logger: DokkaLogger
    ): Pair<DokkaConfiguration, DokkaContext> {
        logger.progress("Initializing and verifying project $this")
        this.verify()
        require(outputDirectory.isDirectory) {
            "outputDirectory has to exist and be a directory: $outputDirectory"
        }
        this.initializeTestFiles(relativeToDir = outputDirectory, logger)

        logger.progress("Creating configuration and context")
        val testDokkaConfiguration = this.getConfiguration()
        val dokkaConfiguration = testDokkaConfiguration.toDokkaConfiguration(projectDir = outputDirectory).also {
            it.verify()
        }
        return dokkaConfiguration to createContext(dokkaConfiguration, logger, getPluginList())
    }

    /**
     * Takes the virtual [TestDataFile] of this [TestProject] and creates
     * the real files relative to the [relativeToDir] param.
     */
    private fun TestProject.initializeTestFiles(relativeToDir: File, logger: DokkaLogger) {
        logger.progress("Initializing test files relative to the \"$relativeToDir\" directory")

        this.getTestData().getFiles().forEach {
            val testDataFile = relativeToDir.resolve(it.pathFromProjectRoot.removePrefix("/"))
            try {
                testDataFile.parentFile.mkdirs()
            } catch (e: Exception) {
                // the IOException thrown from `mkdirs()` has no details and thus is more difficult to debug.
                throw IllegalStateException("Unable to create dirs \"${testDataFile.parentFile}\"", e)
            }

            logger.debug("Creating \"${testDataFile.absolutePath}\"")
            check(testDataFile.createNewFile()) {
                "Unable to create a test file: ${testDataFile.absolutePath}"
            }
            testDataFile.writeText(it.getContents(), Charsets.UTF_8)
        }
    }

    /**
     * Verifies this [DokkaConfiguration] to make sure there are no unexpected
     * parameter option values, such as non-existing classpath entries.
     *
     * If this method fails, it's likely there's a configuration error in the test,
     * or an exception must be made in one of the checks.
     */
    private fun DokkaConfiguration.verify() {
        this.includes.forEach { verifyFileExists(it) }
        this.sourceSets.forEach { sourceSet ->
            sourceSet.classpath.forEach { verifyFileExists(it) }
            sourceSet.includes.forEach { verifyFileExists(it) }
            sourceSet.samples.forEach { verifyFileExists(it) }
            // we do not verify sourceRoots since the source directory
            // is not guaranteed to exist even if it was configured.
        }
    }

    private fun verifyFileExists(file: File) {
        if (!file.exists() && !file.absolutePath.contains("non-existing")) {
            throw IllegalArgumentException(
                "The provided file does not exist. Bad test data or configuration? " +
                        "If it is done intentionally, add \"non-existing\" to the path or the name. File: \"$file\""
            )
        }
    }

    private fun createContext(
        dokkaConfiguration: DokkaConfiguration,
        logger: DokkaLogger,
        pluginOverrides: List<DokkaPlugin>
    ): DokkaContext {
        logger.progress("Creating DokkaContext from test configuration")
        return DokkaContext.create(
            configuration = dokkaConfiguration,
            logger = logger,
            pluginOverrides = pluginOverrides
        )
    }

    /**
     * Generates the documentable model by using all available [SourceToDocumentableTranslator] extensions,
     * and then merging all the results into a single [DModule] by calling [DocumentableMerger].
     */
    private fun generateDocumentableModel(context: DokkaContext, logger: DokkaLogger): DModule {
        logger.progress("Generating the documentable model")
        val sourceSetModules = context
            .configuration
            .sourceSets
            .map { sourceSet -> translateSources(sourceSet, context, logger) }
            .flatten()

        if (sourceSetModules.isEmpty()) {
            throw IllegalStateException("Got no modules after translating sources. Is the test data set up?")
        }

        return DefaultDocumentableMerger(context).invoke(sourceSetModules)
            ?: error("Unable to merge documentables for some reason")
    }

    /**
     * Translates input source files to the documentable model by using
     * all registered [SourceToDocumentableTranslator] core extensions.
     */
    private fun translateSources(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        context: DokkaContext,
        logger: DokkaLogger
    ): List<DModule> {
        val translators = context[CoreExtensions.sourceToDocumentableTranslator]
        require(translators.isNotEmpty()) {
            "Need at least one source to documentable translator to run tests, otherwise no data will be generated."
        }
        logger.debug("Translating sources for ${sourceSet.sourceSetID}")
        return translators.map { it.invoke(sourceSet, context) }
    }

    /**
     * A helper function to query analysis services, to avoid
     * boilerplate and misconfiguration in the tests.
     *
     * The idea is to provide the users with ready-to-use services,
     * without them having to know how to query or configure them.
     */
    private fun createTestAnalysisServices(
        context: DokkaContext,
        logger: DokkaLogger
    ): TestAnalysisServices {
        logger.progress("Creating analysis services")
        val publicAnalysisPlugin = context.plugin<KotlinAnalysisPlugin>()
        val internalAnalysisPlugin = context.plugin<InternalKotlinAnalysisPlugin>()
        return TestAnalysisServices(
            sampleAnalysisEnvironmentCreator = publicAnalysisPlugin.querySingle { sampleAnalysisEnvironmentCreator },
            externalDocumentableProvider = publicAnalysisPlugin.querySingle { externalDocumentableProvider },
            moduleAndPackageDocumentationReader = internalAnalysisPlugin.querySingle {
                moduleAndPackageDocumentationReader
            }
        )
    }

    /**
     * Cleans up memory used during analysis by invoking all Dokka post-actions
     * which will dispose KotlinAnalysis sessions for both K1 and K2 analysis.
     * After this, [context] should not be used.
     */
    private fun cleanup(context: DokkaContext) {
        context[CoreExtensions.postActions].forEach { action -> action.invoke() }
    }

}
