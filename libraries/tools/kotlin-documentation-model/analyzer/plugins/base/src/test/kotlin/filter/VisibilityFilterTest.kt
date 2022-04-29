package filter

import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.DokkaDefaults
import org.jetbrains.dokka.PackageOptionsImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DModule
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisibilityFilterTest : BaseAbstractTest() {

    @Test
    fun `should document only public for defaults`() {
        testVisibility(
            """
            | val publicProperty: String = "publicProperty"
            | private val privateProperty: String = "privateProperty" 
            |
            | fun publicFun() { }
            | private fun privateFun() { } 
            """.trimIndent(),
            visibilities = DokkaDefaults.documentedVisibilities
        ) { module ->
            val pckg = module.first().packages.first()
            pckg.properties.also {
                assertEquals(1, it.size)
                assertEquals("publicProperty", it[0].name)
            }
            pckg.functions.also {
                assertEquals(1, it.size)
                assertEquals("publicFun", it[0].name)
            }
        }
    }

    @Test
    fun `should document public`() {
        testVisibility(
            """
            | class TestClass {
            |     val publicProperty: String = "publicProperty"
            |     internal val noise: String = "noise"
            |
            |     private val privateProperty: String = "privateProperty"
            |
            |     fun publicFun() { }
            |
            |     private fun privateFun() { }
            | }
            """.trimIndent(),
            visibilities = setOf(Visibility.PUBLIC)
        ) { module ->
            val clazz = module.first().packages.first().classlikes.filterIsInstance<DClass>().first()
            clazz.properties.also {
                assertEquals(1, it.size)
                assertEquals("publicProperty", it[0].name)
            }
            clazz.functions.also {
                assertEquals(1, it.size)
                assertEquals("publicFun", it[0].name)
            }
        }
    }

    @Test
    fun `should document only private`() {
        testVisibility(
            """
            | public val noiseMember: String = "noise"
            | internal fun noiseFun() { }
            | class NoisePublicClass { }
            |
            | private val privateProperty: String = "privateProperty"
            | private fun privateFun() { }
            """.trimIndent(),
            visibilities = setOf(Visibility.PRIVATE)
        ) { module ->
            val pckg = module.first().packages.first()

            assertTrue(pckg.classlikes.isEmpty())
            pckg.properties.also {
                assertEquals(1, it.size)
                assertEquals("privateProperty", it[0].name)
            }
            pckg.functions.also {
                assertEquals(1, it.size)
                assertEquals("privateFun", it[0].name)
            }
        }
    }

    @Test
    fun `should document only internal`() {
        testVisibility(
            """
            | public val noiseMember: String = "noise"
            | private fun noiseFun() { }
            | class NoisePublicClass { }
            |
            | internal val internalProperty: String = "privateProperty"
            | internal fun internalFun() { }
            """.trimIndent(),
            visibilities = setOf(Visibility.INTERNAL)
        ) { module ->
            val pckg = module.first().packages.first()

            assertTrue(pckg.classlikes.isEmpty())
            pckg.properties.also {
                assertEquals(1, it.size)
                assertEquals("internalProperty", it[0].name)
            }
            pckg.functions.also {
                assertEquals(1, it.size)
                assertEquals("internalFun", it[0].name)
            }
        }
    }

    @Test
    fun `should document private within public class`() {
        testVisibility(
            """
            | class TestClass {
            |     val publicProperty: String = "publicProperty"
            |     internal val noise: String = "noise"
            |
            |     private val privateProperty: String = "privateProperty"
            |
            |     fun publicFun() { }
            |
            |     private fun privateFun() { }
            | }
            """.trimIndent(),
            visibilities = setOf(Visibility.PUBLIC, Visibility.PRIVATE)
        ) { module ->
            val clazz = module.first().packages.first().classlikes.filterIsInstance<DClass>().first()
            clazz.properties.also {
                assertEquals(2, it.size)
                assertEquals("publicProperty", it[0].name)
                assertEquals("privateProperty", it[1].name)
            }
            clazz.functions.also {
                assertEquals(2, it.size)
                assertEquals("publicFun", it[0].name)
                assertEquals("privateFun", it[1].name)
            }
        }
    }

    @Test
    fun `should document internal within public class`() {
        testVisibility(
            """
            | class TestClass {
            |     val publicProperty: String = "publicProperty"
            |     protected val noise: String = "noise"
            |
            |     internal val internalProperty: String = "internalProperty"
            |
            |     fun publicFun() { }
            |
            |     internal fun internalFun() { }
            | }
            """.trimIndent(),
            visibilities = setOf(Visibility.PUBLIC, Visibility.INTERNAL)
        ) { module ->
            val clazz = module.first().packages.first().classlikes.filterIsInstance<DClass>().first()
            clazz.properties.also {
                assertEquals(2, it.size)
                assertEquals("publicProperty", it[0].name)
                assertEquals("internalProperty", it[1].name)
            }
            clazz.functions.also {
                assertEquals(2, it.size)
                assertEquals("publicFun", it[0].name)
                assertEquals("internalFun", it[1].name)
            }
        }
    }

    @Test
    fun `should document protected within public class`() {
        testVisibility(
            """
            | class TestClass {
            |     val publicProperty: String = "publicProperty"
            |     internal val noise: String = "noise"
            |
            |     protected val protectedProperty: String = "protectedProperty"
            |
            |     fun publicFun() { }
            |
            |     protected fun protectedFun() { }
            | }
            """.trimIndent(),
            visibilities = setOf(Visibility.PUBLIC, Visibility.PROTECTED)
        ) { module ->
            val clazz = module.first().packages.first().classlikes.filterIsInstance<DClass>().first()
            clazz.properties.also {
                assertEquals(2, it.size)
                assertEquals("publicProperty", it[0].name)
                assertEquals("protectedProperty", it[1].name)
            }
            clazz.functions.also {
                assertEquals(2, it.size)
                assertEquals("publicFun", it[0].name)
                assertEquals("protectedFun", it[1].name)
            }
        }
    }

    @Test
    fun `should document all visibilities`() {
        testVisibility(
            """
            | class TestClass {
            |     val publicProperty: String = "publicProperty"
            |
            |     private val privateProperty: String = "privateProperty"
            |     internal val internalProperty: String = "internalProperty"
            |     protected val protectedProperty: String = "protectedProperty"
            |
            |     fun publicFun() { }
            |
            |     private fun privateFun() { }
            |     internal fun internalFun() { }
            |     protected fun protectedFun() { }
            | }
            """.trimIndent(),
            visibilities = setOf(
                Visibility.PUBLIC,
                Visibility.PRIVATE,
                Visibility.PROTECTED,
                Visibility.INTERNAL
            )
        ) { module ->
            val clazz = module.first().packages.first().classlikes.filterIsInstance<DClass>().first()
            clazz.properties.also {
                assertEquals(4, it.size)
                assertEquals("publicProperty", it[0].name)
                assertEquals("privateProperty", it[1].name)
                assertEquals("internalProperty", it[2].name)
                assertEquals("protectedProperty", it[3].name)
            }
            clazz.functions.also {
                assertEquals(4, it.size)
                assertEquals("publicFun", it[0].name)
                assertEquals("privateFun", it[1].name)
                assertEquals("internalFun", it[2].name)
                assertEquals("protectedFun", it[3].name)
            }
        }
    }

    @Test
    fun `should ignore visibility settings for another package`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    perPackageOptions = listOf(
                        PackageOptionsImpl(
                            matchingRegex = "other",
                            documentedVisibilities = setOf(Visibility.PRIVATE),
                            includeNonPublic = false,
                            reportUndocumented = false,
                            skipDeprecated = false,
                            suppress = false
                        )
                    )
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            | fun publicFun() { }
            |
            | private fun privateFun() { }
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                val functions = it.first().packages.first().functions
                assertEquals(1, functions.size)
                assertEquals("publicFun", functions[0].name)
            }
        }
    }

    @Test
    fun `should choose package visibility settings over global`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    documentedVisibilities = setOf(Visibility.INTERNAL)
                    perPackageOptions = listOf(
                        PackageOptionsImpl(
                            matchingRegex = "example",
                            documentedVisibilities = setOf(Visibility.PRIVATE),
                            includeNonPublic = false,
                            reportUndocumented = false,
                            skipDeprecated = false,
                            suppress = false
                        )
                    )
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            | internal fun internalFun() { }
            | 
            | private fun privateFun() { }
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                val functions = it.first().packages.first().functions
                assertEquals(1, functions.size)
                assertEquals("privateFun", functions[0].name)
            }
        }
    }

    @Test
    fun `private setter should be hidden if only PUBLIC is documented`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    documentedVisibilities = setOf(Visibility.PUBLIC)
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |var property: Int = 0
            |private set 
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertNull(
                    it.first().packages.first().properties.first().setter
                )
            }
        }
    }

    @Test
    fun `should choose new documentedVisibilities over deprecated includeNonPublic`() {
        @Suppress("DEPRECATION")
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = true
                    documentedVisibilities = setOf(Visibility.INTERNAL)
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            | internal fun internalFun() { }
            | 
            | private fun privateFun() { }
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                val functions = it.first().packages.first().functions
                assertEquals(1, functions.size)
                assertEquals("internalFun", functions[0].name)
            }
        }
    }

    @Test
    fun `includeNonPublic - public function with false global`() {
        @Suppress("DEPRECATION")
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = false
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |fun testFunction() { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.size == 1
                )
            }
        }
    }

    @Test
    fun `includeNonPublic - private function with false global`() {
        @Suppress("DEPRECATION")
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = false
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |private fun testFunction() { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.isEmpty()
                )
            }
        }
    }

    @Test
    fun `includeNonPublic - private function with true global`() {
        @Suppress("DEPRECATION")
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                    includeNonPublic = true
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |private fun testFunction() { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.size == 1
                )
            }
        }
    }

    @Test
    fun `private setter with false global includeNonPublic`() {
        @Suppress("DEPRECATION")
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = false
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |var property: Int = 0
            |private set 
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertNull(
                    it.first().packages.first().properties.first().setter
                )
            }
        }
    }

    @Test
    fun `includeNonPublic - private function with false global true package`() {
        @Suppress("DEPRECATION")
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                    includeNonPublic = false
                    perPackageOptions = mutableListOf(
                        PackageOptionsImpl(
                            "example",
                            true,
                            false,
                            false,
                            false,
                            DokkaDefaults.documentedVisibilities
                        )
                    )
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |private fun testFunction() { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.size == 1
                )
            }
        }
    }

    @Test
    fun `includeNonPublic - private function with true global false package`() {
        @Suppress("DEPRECATION")
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                    includeNonPublic = true
                    perPackageOptions = mutableListOf(
                        PackageOptionsImpl(
                            "example",
                            false,
                            false,
                            false,
                            false,
                            DokkaDefaults.documentedVisibilities
                        )
                    )
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |private fun testFunction() { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.isEmpty()
                )
            }
        }
    }

    @Test
    fun `includeNonPublic - private typealias should be skipped`() {
        @Suppress("DEPRECATION")
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = false
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |private typealias ABC = Int
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                assertEquals(0, it.first().packages.first().typealiases.size)
            }
        }
    }

    @Test
    fun `includeNonPublic - internal property from enum should be skipped`() {
        @Suppress("DEPRECATION")
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = false
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package enums
            |
            |enum class Test(internal val value: Int) {
            |    A(0) {
            |        override fun testFun(): Float = 0.05F
            |    },
            |    B(1) {
            |        override fun testFun(): Float = 0.1F
            |    };
            | 
            |    internal open fun testFun(): Float = 0.5F
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { module ->
                val enum = module.packages.flatMap { it.classlikes }.filterIsInstance<DEnum>().first()
                val entry = enum.entries.first()

                assertFalse("testFun" in entry.functions.map { it.name })
                assertFalse("value" in entry.properties.map { it.name })
                assertFalse("testFun" in enum.functions.map { it.name })
            }
        }
    }

    @Test
    fun `includeNonPublic - internal property from enum`() {
        @Suppress("DEPRECATION")
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = true
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package enums
            |
            |enum class Test(internal val value: Int) {
            |    A(0) {
            |        override fun testFun(): Float = 0.05F
            |    },
            |    B(1) {
            |        override fun testFun(): Float = 0.1F
            |    };
            | 
            |    internal open fun testFun(): Float = 0.5F
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { module ->
                val enum = module.packages.flatMap { it.classlikes }.filterIsInstance<DEnum>().first()
                val entry = enum.entries.first()

                assertTrue("testFun" in entry.functions.map { it.name })
                assertTrue("value" in entry.properties.map { it.name })
                assertTrue("testFun" in enum.functions.map { it.name })
            }
        }
    }


    private fun testVisibility(
        body: String,
        visibilities: Set<Visibility>,
        asserts: (List<DModule>) -> Unit
    ) {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    documentedVisibilities = visibilities
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            $body
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = asserts
        }
    }
}
