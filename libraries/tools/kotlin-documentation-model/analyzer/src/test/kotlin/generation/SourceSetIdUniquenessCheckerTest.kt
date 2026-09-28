/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package generation

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.base.generation.SingleModuleGeneration
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.plugability.DokkaContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourceSetIdUniquenessCheckerTest : BaseAbstractTest() {
    @Test
    fun `pre-generation check should fail if there are sourceSets with the same id`() {
        val configuration = dokkaConfiguration {
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
        val context = DokkaContext.create(configuration, logger, emptyList())
        val generation = context.single(CoreExtensions.generation) as SingleModuleGeneration

        val exception = assertFailsWith<DokkaException> { generation.validityCheck(context) }
        assertEquals(
            exception.message,
            "Pre-generation validity check failed: Source sets 'S1' and 'S2' have the same `sourceSetID=root/JVM`. Every source set should have unique sourceSetID."
        )
    }

    @Test
    fun `pre-generation check should not fail if sourceSets have different ids`() {
        val configuration = dokkaConfiguration {
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
        val context = DokkaContext.create(configuration, logger, emptyList())
        val generation = context.single(CoreExtensions.generation) as SingleModuleGeneration

        // check no error thrown
        // assertEquals is needed not to have a dangling declaration, it has `assertNotFails` semantics.
        assertEquals(generation.validityCheck(context), Unit)
    }
}
