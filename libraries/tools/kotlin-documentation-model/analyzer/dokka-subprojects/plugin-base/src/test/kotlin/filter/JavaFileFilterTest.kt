/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package filter

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import kotlin.test.Test
import kotlin.test.assertTrue

class JavaFileFilterTest : BaseAbstractTest() {
    @Test
    fun `java file should be included`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    skipEmptyPackages = false
                    sourceRoots = listOf("src/main/java/basic/Test.java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/basic/Test.java
            |package example;
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                assertTrue(
                    it.first().packages.isNotEmpty()
                )
            }
        }
    }
}
