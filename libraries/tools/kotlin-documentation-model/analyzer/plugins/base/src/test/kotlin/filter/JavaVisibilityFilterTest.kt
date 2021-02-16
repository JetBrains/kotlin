package filter

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.PackageOptionsImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import testApi.testRunner.dokkaConfiguration
import kotlin.test.assertEquals

class JavaVisibilityFilterTest : BaseAbstractTest() {
    @ParameterizedTest
    @MethodSource(value = ["nonPublicPermutations", "publicPermutations"])
    fun `should include package private java class`(configuration: ConfigurationWithVisibility) {
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
        val globalExcludes = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = false
                    sourceRoots = listOf("src/")
                }
            }
        }

        val globalIncludes = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = true
                    sourceRoots = listOf("src/")
                }
            }
        }

        val globalIncludesPackageExcludes = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = true
                    sourceRoots = listOf("src/")
                    perPackageOptions = mutableListOf(
                        PackageOptionsImpl(
                            "basic",
                            false,
                            false,
                            false,
                            false
                        )
                    )
                }
            }
        }

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
                            false
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