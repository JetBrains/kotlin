/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.plugins.JavaPluginExtension
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Assume
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

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

        val mainJvmCompilation = project.multiplatformExtension.jvm().compilations
            .getByName(KotlinCompilation.MAIN_COMPILATION_NAME) as KotlinJvmCompilation
        runTask.assertClasspathContains(mainJvmCompilation.output.allOutputs)
        runTask.assertClasspathContains(mainJvmCompilation.runtimeDependencyFiles)
    }

    @Test
    fun createsDefaultDistribution() {
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

        val scriptsTask = project.tasks.findByName("startScriptsForJvm") as? CreateStartScripts
        val installTask = project.tasks.findByName("installJvmDist") as? Sync
        val zipTask = project.tasks.findByName("jvmDistZip") as? Zip
        val tarTask = project.tasks.findByName("jvmDistTar") as? Tar

        assertNotNull(scriptsTask, "Expected 'startScriptsForJvm' task to be created")
        assertNotNull(installTask, "Expected 'installJvmDist' task to be created")
        assertNotNull(zipTask, "Expected 'distZipJvm' task to be created")
        assertNotNull(tarTask, "Expected 'distTarJvm' task to be created")

        val mainJvmCompilation = project.multiplatformExtension
            .jvm()
            .compilations
            .getByName(KotlinCompilation.MAIN_COMPILATION_NAME) as KotlinJvmCompilation

        with(scriptsTask) {
            assertEquals("foo.MainKt", mainClass.get())
            assertEquals(project.name, applicationName)
            assertEquals("bin", executableDir)
            assertEquals(null, mainModule.orNull)
            assertEquals(true, modularity.inferModulePath.get()) // convention is true
            assertEquals(emptyList(), defaultJvmOpts)
            assertEquals(project.projectDir.resolve("build/jvm/scripts"), outputDir)
            assertNotNull(classpath)
            assertClasspathContains(mainJvmCompilation.runtimeDependencyFiles)
            assertClasspathContains(
                project.tasks.getByName(mainJvmCompilation.archiveTaskName!!).outputs.files
            )
        }

        with(installTask) {
            assertEquals(project.projectDir.resolve("build/install/${project.name}-jvm"), destinationDir)
        }

        with(zipTask) {
            assertEquals(
                project.projectDir.resolve("build/distributions/${project.name}-jvm.zip"),
                archiveFile.get().asFile
            )
        }

        with(tarTask) {
            assertEquals(
                project.projectDir.resolve("build/distributions/${project.name}-jvm.tar"),
                archiveFile.get().asFile
            )
        }
    }

    @Test
    fun customApplicationNameIsPropagatedToDistribution() {
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
                            applicationName.set("foo")
                        }
                    }
                }
            }
        }

        project.evaluate()

        val scriptsTask = project.tasks.findByName("startScriptsForJvm") as? CreateStartScripts
        assertNotNull(scriptsTask, "Expected 'startScriptsForJvm' task to be created")
        assertEquals("foo", scriptsTask.applicationName)

        val installTask = project.tasks.findByName("installJvmDist") as? Sync
        assertNotNull(installTask, "Expected 'installJvmDist' task to be created")
        assertEquals(project.projectDir.resolve("build/install/foo-jvm"), installTask.destinationDir)

        val zipTask = project.tasks.findByName("jvmDistZip") as? Zip
        assertNotNull(zipTask, "Expected 'distZipJvm' task to be created")
        assertEquals(
            project.projectDir.resolve("build/distributions/foo-jvm.zip"),
            zipTask.archiveFile.get().asFile
        )

        val tarTask = project.tasks.findByName("jvmDistTar") as? Tar
        assertNotNull(tarTask, "Expected 'distTarJvm' task to be created")
        assertEquals(
            project.projectDir.resolve("build/distributions/foo-jvm.tar"),
            tarTask.archiveFile.get().asFile
        )
    }

    @Test
    fun customExecutableDirIsUsedInScripts() {
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
                            executableDir.set("anotherBin")
                        }
                    }
                }
            }
        }

        project.evaluate()

        val scriptsTask = project.tasks.findByName("startScriptsForJvm") as? CreateStartScripts
        assertNotNull(scriptsTask, "Expected 'startScriptsForJvm' task to be created")
        assertEquals("anotherBin", scriptsTask.executableDir)
    }

    @Test
    fun customJvmArgsAreUsedInScripts() {
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
                            applicationDefaultJvmArgs.add("-Xmx512m")
                            applicationDefaultJvmArgs.add("-Dfoo.bar=baz")
                        }
                    }
                }
            }
        }

        project.evaluate()

        val scriptsTask = project.tasks.findByName("startScriptsForJvm") as? CreateStartScripts
        assertNotNull(scriptsTask, "Expected 'startScriptsForJvm' task to be created")
        assertEquals(listOf("-Xmx512m", "-Dfoo.bar=baz"), scriptsTask.defaultJvmOpts)
    }

    @Test
    fun creatingMultipleDistributions() {
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

                        executable(KotlinCompilation.MAIN_COMPILATION_NAME, "foo") {
                            mainClass.set("foo.MainFooKt")
                            applicationName.set("foo")
                        }

                        executable(KotlinCompilation.TEST_COMPILATION_NAME) {
                            mainClass.set("foo.TestKt")
                        }
                    }
                }
            }
        }

        project.evaluate()

        val scriptsTask = project.tasks.findByName("startScriptsForJvm") as? CreateStartScripts
        assertNotNull(scriptsTask, "Expected 'startScriptsForJvm' task to be created")
        assertEquals(project.name, scriptsTask.applicationName)

        val installTask = project.tasks.findByName("installJvmDist") as? Sync
        assertNotNull(installTask, "Expected 'installJvmDist' task to be created")

        val zipTask = project.tasks.findByName("jvmDistZip") as? Zip
        assertNotNull(zipTask, "Expected 'jvmDistZip' task to be created")

        val tarTask = project.tasks.findByName("jvmDistTar") as? Tar
        assertNotNull(tarTask, "Expected 'jvmDistTar' task to be created")

        val scriptsTaskMainFoo = project.tasks.findByName("startScriptsForJvmFoo") as? CreateStartScripts
        assertNotNull(scriptsTaskMainFoo, "Expected 'startScriptsForJvmFoo' task to be created")
        assertEquals("foo", scriptsTaskMainFoo.applicationName)

        val installTaskMainFoo = project.tasks.findByName("installJvmFooDist") as? Sync
        assertNotNull(installTaskMainFoo, "Expected 'installJvmFooDist' task to be created")

        val zipTaskMainFoo = project.tasks.findByName("jvmFooDistZip") as? Zip
        assertNotNull(zipTaskMainFoo, "Expected 'jvmFooDistZip' task to be created")

        val tarTaskMainFoo = project.tasks.findByName("jvmFooDistTar") as? Tar
        assertNotNull(tarTaskMainFoo, "Expected 'jvmFooDistTar' task to be created")

        val scriptsTaskTest = project.tasks.findByName("startScriptsForJvmTest") as? CreateStartScripts
        assertNotNull(scriptsTaskTest, "Expected 'startScriptsForJvmTest' task to be created")
        assertEquals(project.name, scriptsTaskTest.applicationName)

        val installTaskTest = project.tasks.findByName("installJvmTestDist") as? Sync
        assertNotNull(installTaskTest, "Expected 'installJvmTestDist' task to be created")

        val zipTaskTest = project.tasks.findByName("jvmTestDistZip") as? Zip
        assertNotNull(zipTaskTest, "Expected 'jvmTestDistZip' task to be created")

        val tarTaskTest = project.tasks.findByName("jvmTestDistTar") as? Tar
        assertNotNull(tarTaskTest, "Expected 'jvmTestDistTar' task to be created")
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
        val testCompilation = project.multiplatformExtension.jvm().compilations
            .getByName(KotlinCompilation.TEST_COMPILATION_NAME) as KotlinJvmCompilation
        runTask.assertClasspathContains(testCompilation.output.allOutputs)
        runTask.assertClasspathContains(testCompilation.runtimeDependencyFiles)
        assertNotNull(project.tasks.findByName("jvmTestJar"))
    }

    @Test
    fun createRunTaskForCustomCompilation() {
        val project = buildProjectWithMPP {
            repositories.mavenLocal()

            kotlin {
                jvm {
                    val customCompilation = compilations.register("custom")
                    binaries {
                        executable(customCompilation.name) {
                            mainClass.set("foo.MainKt")
                        }
                    }
                }
            }
        }

        project.evaluate()

        val runTask = project.tasks.findByName("runJvmCustom") as? JavaExec
        assertNotNull(runTask, "Expected 'runJvmCustom' task to be created")
        assertEquals("foo.MainKt", runTask.mainClass.get())
        assertEquals(true, runTask.javaLauncher.get().metadata.isCurrentJvm)
        assertNotNull(project.tasks.findByName("jvmCustomJar"))
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

    @Test
    fun gradleDistributionPluginShouldNotBeAppliedByDefault() {
        val project = buildProjectWithMPP {
            kotlin { jvm() }
        }

        project.evaluate()

        assertFalse(project.plugins.hasPlugin("distribution"), "Gradle 'distribution' plugin should not be applied by default")
    }

    private fun JavaExec.assertClasspathContains(
        expectedFiles: FileCollection,
    ) {
        val missingDependencies = expectedFiles.toSet() - classpath.files
        assertEquals(
            emptySet(),
            missingDependencies,
            "JavaExec task $name should contain all dependencies of this collection: ${expectedFiles.files.joinToString()}," +
                    " but missing: ${missingDependencies.joinToString()}",
        )
    }

    private fun CreateStartScripts.assertClasspathContains(
        expectedFiles: FileCollection,
    ) {
        val missingDependencies = expectedFiles.toSet() - classpath!!.files
        assertEquals(
            emptySet(),
            missingDependencies,
            "CreateStartScripts task $name should contain all dependencies of this collection: ${expectedFiles.files.joinToString()}," +
                    " but missing: ${missingDependencies.joinToString()}",
        )
    }
}
