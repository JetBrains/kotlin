/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.testApi.testRunner

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.ExternalDocumentationLinkImpl
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import testApi.testRunner.TestDokkaConfigurationBuilder
import java.io.File
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

// TODO: take dokka configuration from file
public abstract class AbstractTest<M : TestMethods, T : TestBuilder<M>, D : DokkaTestGenerator<M>>(
    protected val testBuilder: () -> T,
    protected val dokkaTestGenerator: (DokkaConfiguration, DokkaLogger, M, List<DokkaPlugin>) -> D,
    protected val logger: TestLogger,
) {
    protected fun getTestDataDir(name: String): Path {
        return File("src/test/resources/$name").takeIf { it.exists() }?.toPath()
            ?: throw InvalidPathException(name, "Cannot be found")
    }

    /**
     * @param cleanupOutput if set to true, any temporary files will be cleaned up after execution. If set to false,
     *                      it will be left to the user or the OS to delete it. Has no effect if [useOutputLocationFromConfig]
     *                      is also set to true.
     * @param useOutputLocationFromConfig if set to true, output location specified in [DokkaConfigurationImpl.outputDir]
     *                                    will be used. If set to false, a temporary folder will be used instead.
     */
    protected fun testFromData(
        configuration: DokkaConfigurationImpl,
        cleanupOutput: Boolean = true,
        useOutputLocationFromConfig: Boolean = false,
        pluginOverrides: List<DokkaPlugin> = emptyList(),
        block: T.() -> Unit,
    ) {
        if (useOutputLocationFromConfig) {
            runTests(
                configuration = configuration,
                pluginOverrides = pluginOverrides,
                testLogger = logger,
                block = block
            )
        } else {
            withTempDirectory(cleanUpAfterUse = cleanupOutput) { tempDir ->
                if (!cleanupOutput) {
                    logger.info("Output will be generated under: ${tempDir.absolutePath}")
                }
                runTests(
                    configuration = configuration.copy(outputDir = tempDir),
                    pluginOverrides = pluginOverrides,
                    testLogger = logger,
                    block = block
                )
            }
        }
    }

    protected fun testInline(
        query: String,
        configuration: DokkaConfigurationImpl,
        cleanupOutput: Boolean = true,
        pluginOverrides: List<DokkaPlugin> = emptyList(),
        loggerForTest: DokkaLogger = logger,
        block: T.() -> Unit,
    ) {
        withTempDirectory(cleanUpAfterUse = cleanupOutput) { tempDir ->
            if (!cleanupOutput) {
                loggerForTest.info("Output will be generated under: ${tempDir.absolutePath}")
            }

            val fileMap = query.toFileMap()
            fileMap.materializeFiles(tempDir.toPath().toAbsolutePath())

            val newConfiguration = configuration.copy(
                outputDir = tempDir,
                sourceSets = configuration.sourceSets.map { sourceSet ->
                    sourceSet.copy(
                        sourceRoots = sourceSet.sourceRoots.map { file -> tempDir.resolve(file) }.toSet(),
                        suppressedFiles = sourceSet.suppressedFiles.map { file -> tempDir.resolve(file) }.toSet(),
                        sourceLinks = sourceSet.sourceLinks.map {
                            link -> link.copy(localDirectory = tempDir.resolve(link.localDirectory).absolutePath)
                        }.toSet(),
                        includes = sourceSet.includes.map { file -> tempDir.resolve(file) }.toSet()
                    )
                }
            )
            runTests(
                configuration = newConfiguration,
                pluginOverrides = pluginOverrides,
                testLogger = loggerForTest,
                block = block
            )
        }
    }

    private fun withTempDirectory(cleanUpAfterUse: Boolean, block: (tempDirectory: File) -> Unit) {
        val tempDir = this.createTempDir()
        try {
            block(tempDir)
        } finally {
            if (cleanUpAfterUse) {
                tempDir.delete()
            }
        }
    }

    private fun runTests(
        configuration: DokkaConfiguration,
        pluginOverrides: List<DokkaPlugin>,
        testLogger: DokkaLogger = logger,
        block: T.() -> Unit
    ) {
        val testMethods = testBuilder().apply(block).build()
        dokkaTestGenerator(
            configuration,
            testLogger,
            testMethods,
            pluginOverrides
        ).generate()
    }

    private fun String.toFileMap(): Map<String, String> {
        return this.trimIndent().trimMargin()
            .replace("\r\n", "\n")
            .sliceAt(filePathRegex)
            .filter { it.isNotEmpty() && it.isNotBlank() && "\n" in it }
            .map { fileDeclaration -> fileDeclaration.trim() }.associate { fileDeclaration ->
                val filePathAndContent = fileDeclaration.split("\n", limit = 2)
                val filePath = filePathAndContent.first().removePrefix("/").trim()
                val content = filePathAndContent.last().trim()
                filePath to content
            }
    }

    private fun String.sliceAt(regex: Regex): List<String> {
        val matchesStartIndices = regex.findAll(this).toList().map { match -> match.range.first }
        return sequence {
            yield(0)
            yieldAll(matchesStartIndices)
            yield(this@sliceAt.length)
        }
            .zipWithNext { startIndex: Int, endIndex: Int -> substring(startIndex, endIndex) }
            .toList()
            .also { slices ->
                /* Post-condition verifying that no character is lost */
                check(slices.sumBy { it.length } == length)
            }
    }

    private fun Map<String, String>.materializeFiles(
        root: Path = Paths.get("."),
        charset: Charset = Charset.forName("utf-8"),
    ) = this.map { (path, content) ->
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.write(file, content.toByteArray(charset))
    }

    @Suppress("DEPRECATION") // TODO migrate to kotlin.io.path.createTempDirectory with languageVersion >= 1.5
    private fun createTempDir(): File = kotlin.io.createTempDir()

    protected fun dokkaConfiguration(block: TestDokkaConfigurationBuilder.() -> Unit): DokkaConfigurationImpl =
        testApi.testRunner.dokkaConfiguration(block)


    protected val jvmStdlibPath: String? by lazy {
        ClassLoader.getSystemResource("kotlin/jvm/Strictfp.class")
            ?.file
            ?.replace("file:", "")
            ?.replaceAfter(".jar", "")
    }

    protected val jsStdlibPath: String? by lazy {
        ClassLoader.getSystemResource("kotlin/jquery")
            ?.file
            ?.replace("file:", "")
            ?.replaceAfter(".jar", "")
    }

    protected val commonStdlibPath: String? by lazy {
        // TODO: feels hacky, find a better way to do it
        ClassLoader.getSystemResource("kotlin/UInt.kotlin_metadata")
            ?.file
            ?.replace("file:", "")
            ?.replaceAfter(".jar", "")
    }

    protected val stdlibExternalDocumentationLink: ExternalDocumentationLinkImpl = ExternalDocumentationLinkImpl(
        URL("https://kotlinlang.org/api/latest/jvm/stdlib/"),
        URL("https://kotlinlang.org/api/latest/jvm/stdlib/package-list")
    )

    public companion object {
        private val filePathRegex = Regex("""[\n^](\/[\w|\-]+)+(\.\w+)?\s*\n""")
    }
}

public interface TestMethods

public open class CoreTestMethods(
    public open val pluginsSetupStage: (DokkaContext) -> Unit,
    public open val verificationStage: (() -> Unit) -> Unit,
    public open val documentablesCreationStage: (List<DModule>) -> Unit,
    public open val documentablesMergingStage: (DModule) -> Unit,
    public open val documentablesTransformationStage: (DModule) -> Unit,
    public open val pagesGenerationStage: (RootPageNode) -> Unit,
    public open val pagesTransformationStage: (RootPageNode) -> Unit,
    public open val renderingStage: (RootPageNode, DokkaContext) -> Unit,
) : TestMethods

public abstract class TestBuilder<M : TestMethods> {
    public abstract fun build(): M
}

public abstract class DokkaTestGenerator<T : TestMethods>(
    protected val configuration: DokkaConfiguration,
    protected val logger: DokkaLogger,
    protected val testMethods: T,
    protected val additionalPlugins: List<DokkaPlugin> = emptyList(),
) {
    public abstract fun generate()
}
