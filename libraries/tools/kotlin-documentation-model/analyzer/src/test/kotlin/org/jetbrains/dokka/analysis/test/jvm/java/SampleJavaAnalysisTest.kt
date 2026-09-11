/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.jvm.java

import org.jetbrains.dokka.analysis.test.api.javaTestProject
import org.jetbrains.dokka.analysis.test.api.parse
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleJavaAnalysisTest {

    /**
     * Used as a sample for [javaTestProject]
     */
    @Test
    fun sample() {
        val testProject = javaTestProject {
            dokkaConfiguration {
                moduleName = "java-module-name-for-unit-test"

                javaSourceSet {
                    // source-set specific configuration
                }
            }
            javaFile(pathFromSrc = "org/jetbrains/dokka/test/java/Bar.java") {
                +"""
                    public class Bar {
                        public static void bar() {
                            System.out.println("Bar");
                        }
                    }
                """
            }
        }

        val module = testProject.parse()
        assertEquals("java-module-name-for-unit-test", module.name)
        assertEquals(1, module.packages.size)

        val pckg = module.packages[0]
        assertEquals("org.jetbrains.dokka.test.java", pckg.name)
        assertEquals(1, pckg.classlikes.size)

        val fooClass = pckg.classlikes[0]
        assertEquals("Bar", fooClass.name)
    }
}
