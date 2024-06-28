/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.*

class NativeBinaryTest {

    @Test
    fun `test baseNameProvider`() {
        val initialBaseName = "Shared"

        buildProjectWithMPP {
            multiplatformExtension.apply {
                iosSimulatorArm64 {
                    binaries {
                        framework(initialBaseName) {
                            val nameProvider = this.baseNameProvider

                            fun checkBaseName(name: String) {
                                assertEquals(baseName, name)
                                assertEquals(nameProvider.get(), name)
                            }

                            checkBaseName(initialBaseName)

                            val newBaseName = "NewShared"
                            this.baseName = newBaseName
                            checkBaseName(newBaseName)
                        }
                    }
                }
            }
        }
    }
}