package filter

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaDefaults
import org.jetbrains.dokka.PackageOptionsImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import testApi.testRunner.dokkaConfiguration
import kotlin.test.assertEquals

class JavaVisibilityFilterTest : BaseAbstractTest() {

    @Test
    fun `should document nothing private if no visibilities are included`() {
        testVisibility(
            """
            | public class JavaVisibilityTest {
            |     public String publicProperty = "publicProperty";
            |     private String privateProperty = "privateProperty";
            |
            |     public void publicFunction() { }
            |     private void privateFunction() { }
            | }
            """.trimIndent(),
            includedVisibility = DokkaDefaults.documentedVisibilities
        ) { module ->
            val clazz = module.first().packages.first().classlikes.filterIsInstance<DClass>().first()
            clazz.properties.also {
                assertEquals(1, it.size)
                assertEquals("publicProperty", it[0].name)
            }
            clazz.functions.also {
                assertEquals(1, it.size)
                assertEquals("publicFunction", it[0].name)
            }
        }
    }

    @Test
    fun `should document private within public class`() {
        testVisibility(
            """
            | public class JavaVisibilityTest {
            |     public String publicProperty = "publicProperty";
            |     protected String noise = "noise";
            |
            |     private String privateProperty = "privateProperty";
            |
            |     public void publicFunction() { }
            |     private void privateFunction() { }
            | }
            """.trimIndent(),
            includedVisibility = setOf(DokkaConfiguration.Visibility.PUBLIC, DokkaConfiguration.Visibility.PRIVATE)
        ) { module ->
            val clazz = module.first().packages.first().classlikes.filterIsInstance<DClass>().first()
            clazz.properties.also {
                assertEquals(2, it.size)
                assertEquals("publicProperty", it[0].name)
                assertEquals("privateProperty", it[1].name)
            }
            clazz.functions.also {
                assertEquals(2, it.size)
                assertEquals("publicFunction", it[0].name)
                assertEquals("privateFunction", it[1].name)
            }
        }
    }

    @Test
    fun `should document package private within private class`() {
        testVisibility(
            """
            | public class JavaVisibilityTest {
            |     public String publicProperty = "publicProperty";
            |     protected String noise = "noise";
            |
            |     String packagePrivateProperty = "packagePrivateProperty";
            |
            |     public void publicFunction() { }
            |     void packagePrivateFunction() { }
            | }
            """.trimIndent(),
            includedVisibility = setOf(DokkaConfiguration.Visibility.PUBLIC, DokkaConfiguration.Visibility.PACKAGE)
        ) { module ->
            val clazz = module.first().packages.first().classlikes.filterIsInstance<DClass>().first()
            clazz.properties.also {
                assertEquals(2, it.size)
                assertEquals("publicProperty", it[0].name)
                assertEquals("packagePrivateProperty", it[1].name)
            }
            clazz.functions.also {
                assertEquals(2, it.size)
                assertEquals("publicFunction", it[0].name)
                assertEquals("packagePrivateFunction", it[1].name)
            }
        }
    }

    @Test
    fun `should document protected within public class`() {
        testVisibility(
            """
            | public class JavaVisibilityTest {
            |     public String publicProperty = "publicProperty";
            |     String noise = "noise";
            |
            |     protected String protectedProperty = "protectedProperty";
            |
            |     public void publicFunction() { }
            |     protected void protectedFunction() { }
            | }
            """.trimIndent(),
            includedVisibility = setOf(DokkaConfiguration.Visibility.PUBLIC, DokkaConfiguration.Visibility.PROTECTED)
        ) { module ->
            val clazz = module.first().packages.first().classlikes.filterIsInstance<DClass>().first()
            clazz.properties.also {
                assertEquals(2, it.size)
                assertEquals("publicProperty", it[0].name)
                assertEquals("protectedProperty", it[1].name)
            }
            clazz.functions.also {
                assertEquals(2, it.size)
                assertEquals("publicFunction", it[0].name)
                assertEquals("protectedFunction", it[1].name)
            }
        }
    }

    @Test
    fun `should include all visibilities`() {
        testVisibility(
            """
            | public class JavaVisibilityTest {
            |     public String publicProperty = "publicProperty";
            |     private String privateProperty = "privateProperty";
            |     String packagePrivateProperty = "packagePrivateProperty";
            |     protected String protectedProperty = "protectedProperty";
            |
            |     public void publicFunction() { }
            |     private void privateFunction() { }
            |     void packagePrivateFunction() { }
            |     protected void protectedFunction() { }
            | }
            """.trimIndent(),
            includedVisibility = setOf(
                DokkaConfiguration.Visibility.PUBLIC,
                DokkaConfiguration.Visibility.PRIVATE,
                DokkaConfiguration.Visibility.PROTECTED,
                DokkaConfiguration.Visibility.PACKAGE,
            )
        ) { module ->
            val clazz = module.first().packages.first().classlikes.filterIsInstance<DClass>().first()
            clazz.properties.also {
                assertEquals(4, it.size)
                assertEquals("publicProperty", it[0].name)
                assertEquals("privateProperty", it[1].name)
                assertEquals("packagePrivateProperty", it[2].name)
                assertEquals("protectedProperty", it[3].name)
            }
            clazz.functions.also {
                assertEquals(4, it.size)
                assertEquals("publicFunction", it[0].name)
                assertEquals("privateFunction", it[1].name)
                assertEquals("packagePrivateFunction", it[2].name)
                assertEquals("protectedFunction", it[3].name)
            }
        }
    }

    private fun testVisibility(body: String, includedVisibility: Set<DokkaConfiguration.Visibility>, asserts: (List<DModule>) -> Unit) {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    documentedVisibilities = includedVisibility
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/java/basic/JavaVisibilityTest.java
            |package example;
            |
            $body
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = asserts
        }
    }

    @ParameterizedTest
    @MethodSource(value = ["nonPublicPermutations", "publicPermutations"])
    fun `includeNonPublic - should include package private java class`(configuration: ConfigurationWithVisibility) {
        testInline(
            """
            |/src/main/java/basic/VisibilityTest.java
            |package basic;
            |
            |${configuration.visibilityKeyword} class VisibilityTest {
            |    static void test() {
            | 
            |    }
            |}
            """.trimMargin(),
            configuration.configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                assertEquals(configuration.expectedClasslikes, it.first().packages.first().classlikes.size)
            }
        }
    }

    data class ConfigurationWithVisibility(
        val visibilityKeyword: String,
        val configuration: DokkaConfigurationImpl,
        val expectedClasslikes: Int
    )

    companion object TestDataSources {
        @Suppress("DEPRECATION") // for includeNonPublic
        val globalExcludes = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = false
                    sourceRoots = listOf("src/")
                }
            }
        }

        @Suppress("DEPRECATION") // for includeNonPublic
        val globalIncludes = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = true
                    sourceRoots = listOf("src/")
                }
            }
        }

        @Suppress("DEPRECATION") // for includeNonPublic
        val globalIncludesPackageExcludes = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = true
                    sourceRoots = listOf("src/")
                    perPackageOptions = mutableListOf(
                        PackageOptionsImpl(
                            "basic",
                            includeNonPublic = false,
                            reportUndocumented = false,
                            skipDeprecated = false,
                            suppress = false,
                            documentedVisibilities = DokkaDefaults.documentedVisibilities
                        )
                    )
                }
            }
        }

        @Suppress("DEPRECATION") // for includeNonPublic
        val globalExcludesPackageIncludes = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = false
                    sourceRoots = listOf("src/")
                    perPackageOptions = mutableListOf(
                        PackageOptionsImpl(
                            "basic",
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

        @JvmStatic
        fun nonPublicPermutations() = listOf("protected", "", "private").flatMap { keyword ->
            listOf(globalIncludes, globalExcludesPackageIncludes).map { configuration ->
                ConfigurationWithVisibility(keyword, configuration, expectedClasslikes = 1)
            } + listOf(globalExcludes, globalExcludes).map { configuration ->
                ConfigurationWithVisibility(keyword, configuration, expectedClasslikes = 0)
            }
        }

        @JvmStatic
        fun publicPermutations() =
            listOf(globalIncludes, globalExcludesPackageIncludes, globalExcludes, globalExcludes).map { configuration ->
                ConfigurationWithVisibility("public", configuration, expectedClasslikes = 1)
            }
    }
}