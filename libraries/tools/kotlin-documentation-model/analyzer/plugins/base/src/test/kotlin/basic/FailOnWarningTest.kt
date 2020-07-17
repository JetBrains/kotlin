package basic

import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import testApi.logger.TestLogger

class FailOnWarningTest : AbstractCoreTest() {

    @Test
    fun `throws exception if one or more warnings were emitted`() {
        val configuration = dokkaConfiguration {
            failOnWarning = true
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
        }

        assertThrows<DokkaException> {
            testInline(
                """
                |/src/main/kotlin
                |package sample
                """.trimIndent(), configuration
            ) {
                pluginsSetupStage = {
                    logger.warn("Warning!")
                }
            }
        }
    }

    @Test
    fun `throws exception if one or more error were emitted`() {
        val configuration = dokkaConfiguration {
            failOnWarning = true
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
        }

        assertThrows<DokkaException> {
            testInline(
                """
                |/src/main/kotlin
                |package sample
                """.trimIndent(), configuration
            ) {
                pluginsSetupStage = {
                    logger.error("Error!")
                }
            }
        }
    }

    @Test
    fun `does not throw if now warning or error was emitted`() {
        logger = TestLogger(ZeroErrorOrWarningCountDokkaLogger())

        val configuration = dokkaConfiguration {
            failOnWarning = true
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
        }


        testInline(
            """
                |/src/main/kotlin
                |package sample
                """.trimIndent(), configuration
        ) {
            /* We expect no Exception */
        }
    }

    @Test
    fun `does not throw if disabled`() {
        val configuration = dokkaConfiguration {
            failOnWarning = false
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
        }


        testInline(
            """
                |/src/main/kotlin
                |package sample
                """.trimIndent(), configuration
        ) {
            pluginsSetupStage = {
                logger.warn("Error!")
                logger.error("Error!")
            }
        }
    }
}

private class ZeroErrorOrWarningCountDokkaLogger(
    logger: DokkaLogger = DokkaConsoleLogger
) : DokkaLogger by logger {
    override var warningsCount: Int = 0
    override var errorsCount: Int = 0
}
