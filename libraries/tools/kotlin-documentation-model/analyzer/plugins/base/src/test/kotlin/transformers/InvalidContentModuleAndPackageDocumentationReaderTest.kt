package transformers

import org.jetbrains.dokka.base.transformers.documentables.ModuleAndPackageDocumentationReader
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testApi.testRunner.TestDokkaConfigurationBuilder
import testApi.testRunner.dModule
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class InvalidContentModuleAndPackageDocumentationReaderTest : AbstractContextModuleAndPackageDocumentationReaderTest() {

    private val includeA by lazy { temporaryDirectory.resolve("includeA.md").toFile() }
    private val includeB by lazy { temporaryDirectory.resolve("includeB.md").toFile() }

    @BeforeEach
    fun materializeInclude() {
        includeA.writeText(
            """
            Invalid random stuff 
            
            # Module moduleA
            Simple stuff
            """.trimIndent()
        )
        includeB.writeText(
            """
            # Module moduleB
            ###
            """.trimIndent()
        )
    }

    private val configurationBuilderA = TestDokkaConfigurationBuilder().apply {
        moduleName = "moduleA"
    }
    private val configurationBuilderB = TestDokkaConfigurationBuilder().apply {
        moduleName = "moduleB"
    }

    private val sourceSetA by configurationBuilderA.sourceSet {
        includes = listOf(includeA.canonicalPath)
    }

    private val sourceSetB by configurationBuilderB.sourceSet {
        includes = listOf(includeB.canonicalPath)
    }

    private val contextA by lazy {
        DokkaContext.create(
            configuration = configurationBuilderA.build(),
            logger = DokkaConsoleLogger(LoggingLevel.DEBUG),
            pluginOverrides = emptyList()
        )
    }
    private val contextB by lazy {
        DokkaContext.create(
            configuration = configurationBuilderB.build(),
            logger = DokkaConsoleLogger(LoggingLevel.DEBUG),
            pluginOverrides = emptyList()
        )
    }

    private val readerA by lazy { ModuleAndPackageDocumentationReader(contextA) }
    private val readerB by lazy { ModuleAndPackageDocumentationReader(contextB) }


    @Test
    fun `parsing should fail with a message when documentation is in not proper format`() {
        val exception =
            runCatching { readerA[dModule(name = "moduleA", sourceSets = setOf(sourceSetA))] }.exceptionOrNull()
        assertEquals(
            "Unexpected classifier: \"Invalid\", expected either \"Module\" or \"Package\". \n" +
                    "For more information consult the specification: https://kotlinlang.org/docs/reference/dokka-module-and-package-docs.html",
            exception?.message
        )
    }

    @Test
    fun `parsing should fail with a message where it encountered error and why`() {
        val exception =
            runCatching { readerB[dModule(name = "moduleB", sourceSets = setOf(sourceSetB))] }.exceptionOrNull()?.message!!

        //I don't want to assert whole message since it contains a path to a temporary folder
        assertTrue(exception.contains("Wrong AST Tree. Header does not contain expected content in "))
        assertTrue(exception.contains("includeB.md"))
        assertTrue(exception.contains("element starts from offset 0 and ends 3: ###"))
    }
}

