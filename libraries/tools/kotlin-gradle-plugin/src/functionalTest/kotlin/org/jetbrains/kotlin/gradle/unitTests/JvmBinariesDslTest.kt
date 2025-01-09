/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.plugins.JavaPluginExtension
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Assume
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JvmBinariesDslTest {

    @Test
    fun createsDefaultRunTask() {
        val project = buildProjectWithMPP {
            repositories {
                mavenLocal()
                mavenCentral()
            }
            kotlin {
                jvm {
                    binaries {
                        executable {
                            mainClass.set("foo.MainKt")
                        }
                    }
                }
            }
        }

        project.evaluate()

        val runTask = project.tasks.findByName("runJvm") as? JavaExec
        assertNotNull(runTask, "Expected 'runJvm' task to be created")
        assertEquals("foo.MainKt", runTask.mainClass.get())
        assertEquals(true, runTask.javaLauncher.get().metadata.isCurrentJvm)
        assertTrue(
            runTask.classpath.files
                .normalizeRelativeToProjectOrRepository(project.projectDir)
                .containsAll(
                    listOf(
                        "build/classes/kotlin/jvm/main".osVariantSeparatorsPathString,
                        "build/processedResources/jvm/main".osVariantSeparatorsPathString,
                    )
                )
        )
    }

    @Test
    fun createRunTaskForTestCompilation() {
        val project = buildProjectWithMPP {
            repositories {
                mavenLocal()
                mavenCentral()
            }

            kotlin {
                jvm {
                    binaries {
                        executable(KotlinCompilation.TEST_COMPILATION_NAME) {
                            mainClass.set("foo.MainKt")
                        }
                    }
                }
            }
        }

        project.evaluate()

        val runTask = project.tasks.findByName("runJvmTest") as? JavaExec
        assertNotNull(runTask, "Expected 'runJvmTest' task to be created")
        assertEquals("foo.MainKt", runTask.mainClass.get())
        assertEquals(true, runTask.javaLauncher.get().metadata.isCurrentJvm)
        assertTrue(
            runTask.classpath.files
                .normalizeRelativeToProjectOrRepository(project.projectDir)
                .containsAll(
                    listOf(
                        "build/classes/kotlin/jvm/test".osVariantSeparatorsPathString,
                        "build/processedResources/jvm/test".osVariantSeparatorsPathString,
                        "build/classes/kotlin/jvm/main".osVariantSeparatorsPathString,
                        "build/processedResources/jvm/main".osVariantSeparatorsPathString,
                    )
                )
        )
    }

    @Test
    fun configuredToolchainIsAppliedToRunTask() {
        // On Windows toolchain detection is not working correctly in the functional tests
        Assume.assumeTrue(!SystemUtils.IS_OS_WINDOWS)

        val project = buildProjectWithMPP {
            kotlin {
                jvm {
                    binaries {
                        executable {
                            mainClass.set("foo.MainKt")
                        }
                    }
                }
                jvmToolchain(21)
            }
        }

        project.evaluate()

        val runTask = project.tasks.getByName("runJvm") as JavaExec
        assertEquals("21", runTask.javaLauncher.get().metadata.jvmVersion.substringBefore('.'))
    }

    @Test
    fun configureJPMSCorrectly() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm {
                    withJava()
                    binaries {
                        executable {
                            mainModule.set("foo.main")
                            mainClass.set("foo.MainKt")
                        }
                    }
                }
            }

            project.extensions.configure(JavaPluginExtension::class.java) {
                it.modularity.inferModulePath.set(true)
            }
        }

        project.evaluate()

        val runTask = project.tasks.getByName("runJvm") as JavaExec
        assertEquals("foo.MainKt", runTask.mainClass.get())
        assertEquals("foo.main", runTask.mainModule.get())
        assertEquals(true, runTask.modularity.inferModulePath.get())
        assertEquals(true, runTask.javaLauncher.get().metadata.isCurrentJvm)
    }

    @Test
    fun possibleToConfigureSeveralBinariesForTheSameCompilation() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm {
                    binaries {
                        executable {
                            mainClass.set("foo.MainKt")
                        }

                        executable("main", "another") {
                            mainClass.set("foo.MainAnotherKt")
                        }
                    }
                }
            }
        }

        project.evaluate()

        val runTask = project.tasks.findByName("runJvm") as? JavaExec
        assertNotNull(runTask, "Expected 'runJvm' task to be created")
        assertEquals("foo.MainKt", runTask.mainClass.get())

        val runTaskAnother = project.tasks.findByName("runJvmAnother") as? JavaExec
        assertNotNull(runTaskAnother, "Expected 'runJvmMainAnother' task to be created")
        assertEquals("foo.MainAnotherKt", runTaskAnother.mainClass.get())
    }

    private fun Iterable<File>.normalizeRelativeToProjectOrRepository(
        projectDir: File
    ) = map {
        if (it.path.startsWith(projectDir.path))
            it.relativeTo(projectDir).path
        else if (it.path.endsWith(".jar")) {
            it.path.substringAfterLast(File.separatorChar)
        } else it
    }
}
