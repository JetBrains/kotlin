/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.jvm.kotlin

import org.jetbrains.dokka.analysis.test.api.kotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.parse
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleKotlinJvmAnalysisTest {

    /**
     * Used as a sample for [kotlinJvmTestProject]
     */
    @Test
    fun sample() {
        val testProject = kotlinJvmTestProject {
            dokkaConfiguration {
                moduleName = "kotlin-jvm-module-name-for-unit-test"

                kotlinSourceSet {
                    // source-set specific configuration
                }
            }
            ktFile(pathFromSrc = "org/jetbrains/dokka/test/kotlin/MyFile.kt") {
                +"public class Foo {}"
            }
        }

        val module = testProject.parse()
        assertEquals("kotlin-jvm-module-name-for-unit-test", module.name)
        assertEquals(1, module.packages.size)

        val pckg = module.packages[0]
        assertEquals("org.jetbrains.dokka.test.kotlin", pckg.name)
        assertEquals(1, pckg.classlikes.size)

        val fooClass = pckg.classlikes[0]
        assertEquals("Foo", fooClass.name)
    }
}
