package transformers

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.transformers.documentables.ModuleAndPackageDocumentationReader
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import testApi.testRunner.TestDokkaConfigurationBuilder
import testApi.testRunner.dModule
import testApi.testRunner.dPackage

class ContextModuleAndPackageDocumentationReaderTest1 : AbstractContextModuleAndPackageDocumentationReaderTest() {


    private val includeSourceSetA by lazy { temporaryDirectory.resolve("includeA.md").toFile() }
    private val includeSourceSetB by lazy { temporaryDirectory.resolve("includeB.md").toFile() }

    @BeforeEach
    fun materializeIncludes() {
        includeSourceSetA.writeText(
            """
            # Module moduleA
            This is moduleA
            
            # Package sample.a
            This is package sample.a\r\n
            
            # Package noise.b
            This will just add some noise
            """.trimIndent().replace("\n", "\r\n")
        )

        includeSourceSetB.writeText(
            """
            # Module moduleB
            This is moduleB
            
            # Package sample.b
            This is package sample.b
            
            # Package noise.b
            This will just add some more noise
            """.trimIndent()
        )
    }

    private val configurationBuilder = TestDokkaConfigurationBuilder().apply {
        moduleName = "moduleA"
    }

    private val sourceSetA by configurationBuilder.sourceSet {
        name = "sourceSetA"
        includes = listOf(includeSourceSetA.canonicalPath)
    }


    private val sourceSetB by configurationBuilder.sourceSet {
        name = "sourceSetB"
        includes = listOf(includeSourceSetB.canonicalPath)
    }


    private val sourceSetB2 by configurationBuilder.sourceSet {
        name = "sourceSetB2"
        includes = emptyList()
    }


    private val context by lazy {
        DokkaContext.create(
            configuration = configurationBuilder.build(),
            logger = TestLogger(DokkaConsoleLogger(LoggingLevel.DEBUG)),
            pluginOverrides = emptyList()
        )
    }

    private val reader by lazy { ModuleAndPackageDocumentationReader(context) }

    @Test
    fun `assert moduleA with sourceSetA`() {
        val documentation = reader[dModule(name = "moduleA", sourceSets = setOf(sourceSetA))]
        assertEquals(
            1, documentation.keys.size,
            "Expected moduleA only containing documentation in a single source set"
        )
        assertEquals(
            "sourceSetA", documentation.keys.single().sourceSetID.sourceSetName,
            "Expected moduleA documentation coming from sourceSetA"
        )

        assertEquals(
            "This is moduleA", documentation.texts.single(),
            "Expected moduleA documentation being present"
        )
    }

    @Test
    fun `assert moduleA with no source sets`() {
        val documentation = reader[dModule("moduleA")]
        assertEquals(
            emptyMap<DokkaSourceSet, DocumentationNode>(), documentation,
            "Expected no documentation received for module not declaring a matching sourceSet"
        )
    }

    @Test
    fun `assert moduleA with unknown source set`() {
        assertThrows<IllegalStateException>(
            "Expected no documentation received for module with unknown sourceSet"
        ) {
            reader[
                    dModule("moduleA", sourceSets = setOf(configurationBuilder.unattachedSourceSet { name = "unknown" }))
            ]
        }
    }

    @Test
    fun `assert moduleA with all sourceSets`() {
        val documentation = reader[dModule("moduleA", sourceSets = setOf(sourceSetA, sourceSetB, sourceSetB2))]
        assertEquals(1, documentation.entries.size, "Expected only one entry from sourceSetA")
        assertEquals(sourceSetA, documentation.keys.single(), "Expected only one entry from sourceSetA")
        assertEquals("This is moduleA", documentation.texts.single())
    }

    @Test
    fun `assert moduleB with sourceSetB and sourceSetB2`() {
        val documentation = reader[dModule("moduleB", sourceSets = setOf(sourceSetB, sourceSetB2))]
        assertEquals(1, documentation.keys.size, "Expected only one entry from sourceSetB")
        assertEquals(sourceSetB, documentation.keys.single(), "Expected only one entry from sourceSetB")
        assertEquals("This is moduleB", documentation.texts.single())
    }

    @Test
    fun `assert sample_A in sourceSetA`() {
        val documentation = reader[dPackage(DRI("sample.a"), sourceSets = setOf(sourceSetA))]
        assertEquals(1, documentation.keys.size, "Expected only one entry from sourceSetA")
        assertEquals(sourceSetA, documentation.keys.single(), "Expected only one entry from sourceSetA")
        assertEquals("This is package sample.a\\r\\n", documentation.texts.single())
    }

    @Test
    fun `assert sample_a_sub in sourceSetA`() {
        val documentation = reader[dPackage(DRI("sample.a.sub"), sourceSets = setOf(sourceSetA))]
        assertEquals(
            emptyMap<DokkaSourceSet, DocumentationNode>(), documentation,
            "Expected no documentation found for different package"
        )
    }

    @Test
    fun `assert sample_a in sourceSetB`() {
        val documentation = reader[dPackage(DRI("sample.a"), sourceSets = setOf(sourceSetB))]
        assertEquals(
            emptyMap<DokkaSourceSet, DocumentationNode>(), documentation,
            "Expected no documentation found for different sourceSet"
        )
    }

    @Test
    fun `assert sample_b in sourceSetB`() {
        val documentation = reader[dPackage(DRI("sample.b"), sourceSets = setOf(sourceSetB))]
        assertEquals(1, documentation.keys.size, "Expected only one entry from sourceSetB")
        assertEquals(sourceSetB, documentation.keys.single(), "Expected only one entry from sourceSetB")
        assertEquals("This is package sample.b", documentation.texts.single())
    }

    @Test
    fun `assert sample_b in sourceSetB and sourceSetB2`() {
        val documentation = reader[dPackage(DRI("sample.b"), sourceSets = setOf(sourceSetB, sourceSetB2))]
        assertEquals(1, documentation.keys.size, "Expected only one entry from sourceSetB")
        assertEquals(sourceSetB, documentation.keys.single(), "Expected only one entry from sourceSetB")
        assertEquals("This is package sample.b", documentation.texts.single())
    }
}
