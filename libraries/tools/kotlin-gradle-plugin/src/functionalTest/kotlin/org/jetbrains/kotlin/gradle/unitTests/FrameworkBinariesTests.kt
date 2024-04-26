/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class FrameworkBinariesTests {

    @Test
    fun `framework output file - reflects link task output file`() {
        buildProjectWithMPP {
            kotlin {
                iosSimulatorArm64 {
                    binaries.framework {
                        assertEquals(
                            "test.framework",
                            linkTaskProvider.get().outputFile.get().name,
                        )
                        assertEquals(
                            "test.framework",
                            outputFile.name,
                        )

                        baseName = "foo"

                        assertEquals(
                            "foo.framework",
                            linkTaskProvider.get().outputFile.get().name,
                        )
                        assertEquals(
                            "foo.framework",
                            outputFile.name,
                        )
                    }
                }
            }
        }
    }

}