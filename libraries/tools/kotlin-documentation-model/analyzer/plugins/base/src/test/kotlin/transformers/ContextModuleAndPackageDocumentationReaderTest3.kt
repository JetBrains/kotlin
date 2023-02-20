package transformers

import org.jetbrains.dokka.base.transformers.documentables.ModuleAndPackageDocumentationReader
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testApi.testRunner.TestDokkaConfigurationBuilder
import testApi.testRunner.dPackage
import kotlin.test.assertEquals

class ContextModuleAndPackageDocumentationReaderTest3 : AbstractContextModuleAndPackageDocumentationReaderTest() {

    private val include by lazy { temporaryDirectory.resolve("include.md").toFile() }

    @BeforeEach
    fun materializeInclude() {
        include.writeText(
            """
            # Package
            This is the root package
            
            # Package [root]
            This is also the root package
            """.trimIndent()
        )
    }

    private val configurationBuilder = TestDokkaConfigurationBuilder()

    private val sourceSet by configurationBuilder.sourceSet {
        includes = listOf(include.canonicalPath)
    }

    private val context by lazy {
        DokkaContext.create(
            configuration = configurationBuilder.build(),
            logger = DokkaConsoleLogger(LoggingLevel.DEBUG),
            pluginOverrides = emptyList()
        )
    }

    private val reader by lazy { ModuleAndPackageDocumentationReader(context) }


    @Test
    fun `root package is matched by empty string and the root keyword`() {
        val documentation = reader[dPackage(DRI(""), sourceSets = setOf(sourceSet))]
        assertEquals(
            listOf("This is the root package", "This is also the root package"), documentation.texts
        )
    }
}
