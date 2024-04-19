/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package generation

import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourceSetIdUniquenessCheckerTest : BaseAbstractTest() {
    @Test
    fun `pre-generation check should fail if there are sourceSets with the same id`() = testInline(
        """
        |/src/main1/file1.kt
        |fun someFunction2()
        |/src/main2/file2.kt
        |fun someFunction2()
        """.trimMargin(),
        dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main1")
                    displayName = "S1"
                    name = "JVM"
                }
                sourceSet {
                    sourceRoots = listOf("src/main2")
                    displayName = "S2"
                    name = "JVM"
                }
            }
        }
    ) {
        verificationStage = { verification ->
            val exception = assertFailsWith(DokkaException::class, verification)
            assertEquals(
                exception.message,
                "Pre-generation validity check failed: Source sets 'S1' and 'S2' have the same `sourceSetID=root/JVM`. Every source set should have unique sourceSetID."
            )
        }
    }

    @Test
    fun `pre-generation check should not fail if sourceSets have different ids`() = testInline(
        """
        |/src/main1/file1.kt
        |fun someFunction2()
        |/src/main2/file2.kt
        |fun someFunction2()
        """.trimMargin(),
        dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main1")
                    displayName = "S1"
                    name = "JVM1"
                }
                sourceSet {
                    sourceRoots = listOf("src/main2")
                    displayName = "S2"
                    name = "JVM2"
                }
            }
        }
    ) {
        verificationStage = { verification ->
            // we check that there is no error thrown
            assertEquals(verification(), Unit)
        }
    }
}
