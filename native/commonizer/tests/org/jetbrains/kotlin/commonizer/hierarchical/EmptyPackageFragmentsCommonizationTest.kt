/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.jetbrains.kotlin.commonizer.getTarget
import org.jetbrains.kotlin.commonizer.parseCommonizerTarget
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmptyPackageFragmentsCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    @Test
    fun `test empty package fragments are not serialized`() {
        val result = commonize {
            outputTarget("(a, b)")

            target("a") {
                module {
                    name = "lib"
                    source(
                        """
                            package foo.bar.baz
                            val baz: Int = 1
                        """.trimIndent(),
                        "baz.kt"
                    )
                    source(
                        """
                            package foo.onlyInA
                            val onlyInA: Int = 1
                        """.trimIndent(),
                        "onlyInA.kt"
                    )
                }
            }

            target("b") {
                module {
                    name = "lib"
                    source(
                        """
                            package foo.bar.baz
                            val baz: Int = 1
                        """.trimIndent(),
                        "baz.kt"
                    )
                }
            }
        }

        result.assertCommonized("(a, b)") {
            name = "lib"
            source(
                """
                    package foo.bar.baz
                    expect val baz: Int
                """.trimIndent()
            )
        }

        val metadata = result.getTarget(parseCommonizerTarget("(a, b)")).single { it.libraryName == "lib" }.metadata
        assertEquals(listOf("foo.bar.baz"), metadata.fragmentNames)
        assertEquals(listOf(1), metadata.fragments.map { it.size })

        val header = KlibMetadataProtoBuf.Header.parseFrom(metadata.module)
        assertEquals(listOf("foo.bar.baz"), header.packageFragmentNameList)
        assertTrue(header.emptyPackageList.isEmpty(), "Unexpected empty packages: ${header.emptyPackageList}")
    }
}
