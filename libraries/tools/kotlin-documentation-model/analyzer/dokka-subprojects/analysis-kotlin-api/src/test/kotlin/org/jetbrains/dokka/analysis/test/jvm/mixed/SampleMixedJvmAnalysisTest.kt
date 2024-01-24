/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.jvm.mixed

import org.jetbrains.dokka.analysis.test.api.mixedJvmTestProject
import org.jetbrains.dokka.analysis.test.api.parse
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleMixedJvmAnalysisTest {

    /**
     * Used as a sample for [mixedJvmTestProject]
     */
    @Test
    fun sample() {
        val testProject = mixedJvmTestProject {
            dokkaConfiguration {
                moduleName = "mixed-project-module-name-for-unit-test"

                jvmSourceSet {
                    // source-set specific configuration
                }
            }

            kotlinSourceDirectory {
                ktFile(pathFromSrc = "test/MyFile.kt") {
                    +"fun foo(): String = \"Foo\""
                }
                javaFile(pathFromSrc = "test/MyJavaFileInKotlin.java") {
                    +"""
                        public class MyJavaFileInKotlin {
                            public static void bar() {
                                System.out.println("Bar");
                            }
                        }
                    """
                }
            }

            javaSourceDirectory {
                ktFile(pathFromSrc = "test/MyFile.kt") {
                    +"fun bar(): String = \"Bar\""
                }
                javaFile(pathFromSrc = "test/MyJavaFileInJava.java") {
                    +"""
                        public class MyJavaFileInJava {
                            public static void bar() {
                                System.out.println("Bar");
                            }
                        }
                    """
                }
            }
        }

        val module = testProject.parse()
        assertEquals("mixed-project-module-name-for-unit-test", module.name)
        assertEquals(1, module.packages.size)

        val pckg = module.packages[0]
        assertEquals("test", pckg.name)

        assertEquals(2, pckg.classlikes.size)
        assertEquals(2, pckg.functions.size)

        val firstClasslike = pckg.classlikes[0]
        assertEquals("MyJavaFileInKotlin", firstClasslike.name)

        val secondClasslike = pckg.classlikes[1]
        assertEquals("MyJavaFileInJava", secondClasslike.name)

        // TODO #3250 address unstable order
        val functions = pckg.functions.sortedBy { it.name }
        val firstFunction = functions[0]
        assertEquals("bar", firstFunction.name)

        val secondFunction = functions[1]
        assertEquals("foo", secondFunction.name)
    }
}
