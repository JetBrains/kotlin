/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.jetbrains.kotlin.commonizer.getTarget
import org.jetbrains.kotlin.commonizer.konan.ModuleSerializer
import org.jetbrains.kotlin.commonizer.parseCommonizerTarget
import org.jetbrains.kotlin.library.assertPackageFragmentsInKlib
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SyntheticPackageFragmentsCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    @Test
    fun `test package fragments are not serialized for intermediate packages`(@TempDir outputDir: File) {
        val result = commonize {
            outputTarget("(linux_arm64, linux_x64)")

            target("linux_arm64") {
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

            target("linux_x64") {
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

        result.assertCommonized("(linux_arm64, linux_x64)") {
            name = "lib"
            source(
                """
                    package foo.bar.baz
                    expect val baz: Int
                """.trimIndent()
            )
        }

        val target = parseCommonizerTarget("(linux_arm64, linux_x64)")
        val moduleResult = result.getTarget(target).single { it.libraryName == "lib" }
        ModuleSerializer(outputDir).consume(result.commonizerParameters, target, moduleResult)

        val klibDir = CommonizerOutputFileLayout.resolveCommonizedDirectory(outputDir, target).resolve(moduleResult.libraryName)
        assertPackageFragmentsInKlib(klibDir.toPath(), expectedPackageFqNames = setOf("foo.bar.baz"))
    }
}
