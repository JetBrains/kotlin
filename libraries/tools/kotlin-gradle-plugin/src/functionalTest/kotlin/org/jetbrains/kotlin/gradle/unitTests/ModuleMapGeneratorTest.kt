/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.ModuleMapGenerator
import org.junit.Test
import kotlin.test.assertEquals

class ModuleMapGeneratorTest {

    @Test
    fun `test modulemap generator`() {
        val modulemap = """
            module "Module_A" {
                umbrella "."
                export *
                link "Module_B"
                link "Module_C"
            }
        """.trimIndent()

        val generated = ModuleMapGenerator.generateModuleMap {
            name = "Module_A"
            umbrella = "."
            export = "*"
            link = listOf("Module_B", "Module_C")
        }

        assertEquals(modulemap, generated)
    }

    @Test
    fun `test framework modulemap generator`() {
        val modulemap = """
            framework module "Module_A" {
                umbrella header "Header.h"
                export *
                module * { export * }
            }
        """.trimIndent()

        val generated = ModuleMapGenerator.generateModuleMap {
            isFramework = true
            isUmbrellaHeader = true

            name = "Module_A"
            umbrella = "Header.h"
            export = "*"
            module = "* { export * }"
        }

        assertEquals(modulemap, generated)
    }
}