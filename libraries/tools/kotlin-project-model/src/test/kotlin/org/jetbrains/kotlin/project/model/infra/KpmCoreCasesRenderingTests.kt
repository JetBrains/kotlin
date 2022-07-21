/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.infra

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KpmCoreCasesRenderingTests : KpmCoreCasesTestRunner {
    @Test
    override fun testSimpleProjectToProject(case: KpmTestCase) {
        case.expectRenderedDsl(
            """
                val SimpleProjectToProject = describeCase("SimpleProjectToProject") {
                    project("a") {
                        module("main") {
                            jvm()
                            macosX64()
                
                            common depends project("b")
                        }
                    }
                
                    project("b") {
                        module("main") {
                            jvm()
                            macosX64()
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    override fun testSimpleTwoLevel(case: KpmTestCase) {
        case.expectRenderedDsl(
            """
            val SimpleTwoLevel = describeCase("SimpleTwoLevel") {
                project("p") {
                    module("main") {
                        jvm()
                    }
                }
            }               
            """.trimIndent()
        )
    }

    private fun KpmTestCase.expectRenderedDsl(@Language("kotlin") expected: String) {
        Assertions.assertEquals(expected.trim(), this.renderDeclarationDsl().trim())
    }
}
