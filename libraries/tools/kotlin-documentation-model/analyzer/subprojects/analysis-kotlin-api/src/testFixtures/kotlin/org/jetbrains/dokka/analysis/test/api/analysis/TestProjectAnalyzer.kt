/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.analysis

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin
import org.jetbrains.dokka.analysis.test.api.TestDataFile
import org.jetbrains.dokka.analysis.test.api.TestProject
import org.jetbrains.dokka.analysis.test.api.configuration.toDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.parse
import org.jetbrains.dokka.analysis.test.api.useServices
import org.jetbrains.dokka.analysis.test.api.util.withTempDirectory
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.documentation.DefaultDocumentableMerger
import org.jetbrains.dokka.transformers.documentation.DocumentableMerger
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import java.io.File

/**
 * The main logger used for running Dokka and analyzing projects.
 *
 * Changing the level to [LoggingLevel.DEBUG] can help with debugging faulty tests
 * or tricky corner cases.
 */
val analysisLogger = DokkaConsoleLogger(minLevel = LoggingLevel.INFO)

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
object TestProjectAnalyzer {

    /**
     * A quick way to analyze a [TestProject], for cases when only the documentable
     * model is needed to verify the result.
     *
     * Creates the input test files, runs Dokka and then deletes them right after the documentable
     * model has been created, leaving no trailing files or any other garbage behind.
     *
     * @see [TestProject.parse] for a user-friendly way to call it
     */
    fun parse(testProject: TestProject): DModule {
        // since we only need documentables, we can delete the input test files right away
        return withTempDirectory(analysisLogger) { tempDirectory ->
            val (_, context) = testProject.initialize(outputDirectory = tempDirectory)
            generateDocumentableModel(context)
        }
    }

    /**
     * Works in the same way as [parse], but it returns the context and configuration used for
     * running Dokka, and does not delete the input test files at the end of the execution - it
     * must be taken care of on call site.
     *
     * @param persistentDirectory a directory that will be used to generate the input test files into.
     *                            It must be available during the test run, especially if services are used,
     *                            otherwise parts of Dokka might not work as expected. Can be safely deleted
     *                            at the end of the test after all asserts have been run.
     *
     * @see [TestProject.useServices] for a user-friendly way to call it
     */
    fun analyze(
        testProject: TestProject,
        persistentDirectory: File
    ): Pair<TestAnalysisServices, TestAnalysisContext> {
        val (dokkaConfiguration, dokkaContext) = testProject.initialize(outputDirectory = persistentDirectory)
        val analysisServices = createTestAnalysisServices(dokkaContext)
        val testAnalysisContext = TestAnalysisContext(
            context = dokkaContext,
            configuration = dokkaConfiguration,
            module = generateDocumentableModel(dokkaContext)
        )
        return analysisServices to testAnalysisContext
    }

    /**
     * Prepares this [TestProject] for analysis by creating
     * the test files, setting up context and configuration.
     */
    private fun TestProject.initialize(outputDirectory: File): Pair<DokkaConfiguration, DokkaContext> {
        analysisLogger.progress("Initializing and verifying project $this")
        this.verify()
        require(outputDirectory.isDirectory) {
            "outputDirectory has to exist and be a directory: $outputDirectory"
        }
        this.initializeTestFiles(relativeToDir = outputDirectory)

        analysisLogger.progress("Creating configuration and context")
        val testDokkaConfiguration = this.getConfiguration()
        val dokkaConfiguration = testDokkaConfiguration.toDokkaConfiguration(projectDir = outputDirectory).also {
            it.verify()
        }
        return dokkaConfiguration to createContext(dokkaConfiguration)
    }

    /**
     * Takes the virtual [TestDataFile] of this [TestProject] and creates
     * the real files relative to the [relativeToDir] param.
     */
    private fun TestProject.initializeTestFiles(relativeToDir: File) {
        analysisLogger.progress("Initializing test files relative to the \"$relativeToDir\" directory")

        this.getTestData().getFiles().forEach {
            val testDataFile = relativeToDir.resolve(it.pathFromProjectRoot.removePrefix("/"))
            try {
                testDataFile.parentFile.mkdirs()
            } catch (e: Exception) {
                // the IOException thrown from `mkdirs()` has no details and thus is more difficult to debug.
                throw IllegalStateException("Unable to create dirs \"${testDataFile.parentFile}\"", e)
            }

            analysisLogger.debug("Creating \"${testDataFile.absolutePath}\"")
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

    private fun createContext(dokkaConfiguration: DokkaConfiguration): DokkaContext {
        analysisLogger.progress("Creating DokkaContext from test configuration")
        return DokkaContext.create(
            configuration = dokkaConfiguration,
            logger = analysisLogger,
            pluginOverrides = listOf()
        )
    }

    /**
     * Generates the documentable model by using all available [SourceToDocumentableTranslator] extensions,
     * and then merging all the results into a single [DModule] by calling [DocumentableMerger].
     */
    private fun generateDocumentableModel(context: DokkaContext): DModule {
        analysisLogger.progress("Generating the documentable model")
        val sourceSetModules = context
            .configuration
            .sourceSets
            .map { sourceSet -> translateSources(sourceSet, context) }
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
    private fun translateSources(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): List<DModule> {
        val translators = context[CoreExtensions.sourceToDocumentableTranslator]
        require(translators.isNotEmpty()) {
            "Need at least one source to documentable translator to run tests, otherwise no data will be generated."
        }
        analysisLogger.debug("Translating sources for ${sourceSet.sourceSetID}")
        return translators.map { it.invoke(sourceSet, context) }
    }

    /**
     * A helper function to query analysis services, to avoid
     * boilerplate and misconfiguration in the tests.
     *
     * The idea is to provide the users with ready-to-use services,
     * without them having to know how to query or configure them.
     */
    private fun createTestAnalysisServices(context: DokkaContext): TestAnalysisServices {
        analysisLogger.progress("Creating analysis services")
        val internalPlugin = context.plugin<InternalKotlinAnalysisPlugin>()
        return TestAnalysisServices(
            sampleProviderFactory = internalPlugin.querySingle { sampleProviderFactory },
            moduleAndPackageDocumentationReader = internalPlugin.querySingle { moduleAndPackageDocumentationReader }
        )
    }
}
