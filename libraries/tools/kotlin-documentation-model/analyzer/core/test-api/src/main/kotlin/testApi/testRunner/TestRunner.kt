package org.jetbrains.dokka.testApi.testRunner

import com.intellij.openapi.application.PathManager
import org.jetbrains.dokka.*
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import org.junit.rules.TemporaryFolder
import testApi.testRunner.TestDokkaConfigurationBuilder
import java.io.File
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

// TODO: take dokka configuration from file
abstract class AbstractTest<M : TestMethods, T : TestBuilder<M>, D : DokkaTestGenerator<M>>(
    protected val testBuilder: () -> T,
    protected val dokkaTestGenerator: (DokkaConfiguration, DokkaLogger, M, List<DokkaPlugin>) -> D,
    protected val logger: TestLogger,
) {
    protected fun getTestDataDir(name: String) =
        File("src/test/resources/$name").takeIf { it.exists() }?.toPath()
            ?: throw InvalidPathException(name, "Cannot be found")

    /**
     * @param useOutputLocationFromConfig if set to true, output location specified in [DokkaConfigurationImpl.outputDir]
     *                                    will be used. If set to false, a temporary folder will be used instead.
     */
    protected fun testFromData(
        configuration: DokkaConfigurationImpl,
        cleanupOutput: Boolean = true,
        useOutputLocationFromConfig: Boolean = false,
        pluginOverrides: List<DokkaPlugin> = emptyList(),
        block: T.() -> Unit
    ) {
        val testMethods = testBuilder().apply(block).build()
        val configurationToUse =
            if (useOutputLocationFromConfig) {
                configuration
            } else {
                val tempDir = getTempDir(cleanupOutput)
                if (!cleanupOutput) {
                    logger.info("Output generated under: ${tempDir.root.absolutePath}")
                }
                configuration.copy(outputDir = tempDir.root)
            }

        dokkaTestGenerator(
            configurationToUse,
            logger,
            testMethods,
            pluginOverrides
        ).generate()
    }

    protected fun testInline(
        query: String,
        configuration: DokkaConfigurationImpl,
        cleanupOutput: Boolean = true,
        pluginOverrides: List<DokkaPlugin> = emptyList(),
        loggerForTest: DokkaLogger = logger,
        block: T.() -> Unit
    ) {
        val testMethods = testBuilder().apply(block).build()
        val testDirPath = getTempDir(cleanupOutput).root.toPath().toAbsolutePath()
        val fileMap = query.toFileMap()
        fileMap.materializeFiles(testDirPath.toAbsolutePath())
        if (!cleanupOutput)
            loggerForTest.info("Output generated under: ${testDirPath.toAbsolutePath()}")
        val newConfiguration = configuration.copy(
            outputDir = testDirPath.toFile(),
            sourceSets = configuration.sourceSets.map { sourceSet ->
                sourceSet.copy(
                    sourceRoots = sourceSet.sourceRoots.map { file ->
                        testDirPath.toFile().resolve(file)
                    }.toSet(),
                    suppressedFiles = sourceSet.suppressedFiles.map { file ->
                        testDirPath.toFile().resolve(file)
                    }.toSet(),
                    sourceLinks = sourceSet.sourceLinks.map { link ->
                        link.copy(
                            localDirectory = testDirPath.toFile().resolve(link.localDirectory).absolutePath
                        )
                    }.toSet(),
                    includes = sourceSet.includes.map { file ->
                        testDirPath.toFile().resolve(file)
                    }.toSet()
                )
            }
        )
        dokkaTestGenerator(
            newConfiguration,
            loggerForTest,
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
        charset: Charset = Charset.forName("utf-8")
    ) = this.map { (path, content) ->
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.write(file, content.toByteArray(charset))
    }

    private fun getTempDir(cleanupOutput: Boolean) =
        if (cleanupOutput) {
            TemporaryFolder().apply { create() }
        } else {
            TemporaryFolderWithoutCleanup().apply { create() }
        }

    /**
     * Creates a temporary folder, but doesn't delete files
     * right after it's been used, delegating it to the OS
     */
    private class TemporaryFolderWithoutCleanup : TemporaryFolder() {
        override fun after() { }
    }

    protected fun dokkaConfiguration(block: TestDokkaConfigurationBuilder.() -> Unit): DokkaConfigurationImpl =
        testApi.testRunner.dokkaConfiguration(block)


    protected val jvmStdlibPath: String? by lazy {
        PathManager.getResourceRoot(Strictfp::class.java, "/kotlin/jvm/Strictfp.class")
    }

    protected val jsStdlibPath: String? by lazy {
        PathManager.getResourceRoot(Any::class.java, "/kotlin/jquery")
    }

    protected val commonStdlibPath: String? by lazy {
        // TODO: feels hacky, find a better way to do it
        ClassLoader.getSystemResource("kotlin/UInt.kotlin_metadata")
            ?.file
            ?.replace("file:", "")
            ?.replaceAfter(".jar", "")
    }

    protected val stdlibExternalDocumentationLink = ExternalDocumentationLinkImpl(
        URL("https://kotlinlang.org/api/latest/jvm/stdlib/"),
        URL("https://kotlinlang.org/api/latest/jvm/stdlib/package-list")
    )

    companion object {
        private val filePathRegex = Regex("""[\n^](\/[\w|\-]+)+(\.\w+)?\s*\n""")
    }
}

interface TestMethods

open class CoreTestMethods(
    open val pluginsSetupStage: (DokkaContext) -> Unit,
    open val verificationStage: (() -> Unit) -> Unit,
    open val documentablesCreationStage: (List<DModule>) -> Unit,
    open val documentablesMergingStage: (DModule) -> Unit,
    open val documentablesTransformationStage: (DModule) -> Unit,
    open val pagesGenerationStage: (RootPageNode) -> Unit,
    open val pagesTransformationStage: (RootPageNode) -> Unit,
    open val renderingStage: (RootPageNode, DokkaContext) -> Unit
) : TestMethods

abstract class TestBuilder<M : TestMethods> {
    abstract fun build(): M
}

abstract class DokkaTestGenerator<T : TestMethods>(
    protected val configuration: DokkaConfiguration,
    protected val logger: DokkaLogger,
    protected val testMethods: T,
    protected val additionalPlugins: List<DokkaPlugin> = emptyList()
) {
    abstract fun generate()
}
