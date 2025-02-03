/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.plugins.JavaPluginExtension
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.onlyJars
import org.junit.Assume
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class JvmBinariesDslTest {

    @Test
    fun createsDefaultRunTask() {
        val project = testMppProject {
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

        val runTask = project.assertContainsTaskInstance<JavaExec>("runJvm")
        assertEquals("foo.MainKt", runTask.mainClass.get())
        assertEquals(true, runTask.javaLauncher.get().metadata.isCurrentJvm)

        val mainJvmCompilation = project.multiplatformExtension.jvm().compilations
            .getByName(KotlinCompilation.MAIN_COMPILATION_NAME) as KotlinJvmCompilation
        runTask.assertClasspathContains(mainJvmCompilation.output.allOutputs)
        runTask.assertClasspathContains(mainJvmCompilation.runtimeDependencyFiles)
    }

    @Test
    fun createsDefaultRunTaskForTargetWithCustomName() {
        val project = testMppProject {
            kotlin {
                jvm("desktop") {
                    binaries {
                        executable {
                            mainClass.set("foo.MainKt")
                        }
                    }
                }
            }
        }

        project.evaluate()

        val runTask = project.assertContainsTaskInstance<JavaExec>("runDesktop")
        assertEquals("foo.MainKt", runTask.mainClass.get())
        assertEquals(true, runTask.javaLauncher.get().metadata.isCurrentJvm)

        val mainJvmCompilation = project.multiplatformExtension.jvm("desktop").compilations
            .getByName(KotlinCompilation.MAIN_COMPILATION_NAME) as KotlinJvmCompilation
        runTask.assertClasspathContains(mainJvmCompilation.output.allOutputs)
        runTask.assertClasspathContains(mainJvmCompilation.runtimeDependencyFiles)
    }

    @Test
    fun createsDefaultDistribution() {
        val project = testMppProject {
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

        val scriptsTask = project.assertContainsTaskInstance<CreateStartScripts>("startScriptsForJvm")
        val installTask = project.assertContainsTaskInstance<Sync>("installJvmDist")
        val zipTask = project.assertContainsTaskInstance<Zip>("jvmDistZip")
        val tarTask = project.assertContainsTaskInstance<Tar>("jvmDistTar")

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
            assertClasspathContains(
                project.tasks.getByName(mainJvmCompilation.archiveTaskName!!).outputs.files
            )
            assertClasspathContains(mainJvmCompilation.runtimeDependencyFiles.onlyJars)
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
        val project = testMppProject {
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

        val scriptsTask = project.assertContainsTaskInstance<CreateStartScripts>("startScriptsForJvm")
        assertEquals("foo", scriptsTask.applicationName)

        val installTask = project.assertContainsTaskInstance<Sync>("installJvmDist")
        assertEquals(project.projectDir.resolve("build/install/foo-jvm"), installTask.destinationDir)

        val zipTask = project.assertContainsTaskInstance<Zip>("jvmDistZip")
        assertEquals(
            project.projectDir.resolve("build/distributions/foo-jvm.zip"),
            zipTask.archiveFile.get().asFile
        )

        val tarTask = project.assertContainsTaskInstance<Tar>("jvmDistTar")
        assertEquals(
            project.projectDir.resolve("build/distributions/foo-jvm.tar"),
            tarTask.archiveFile.get().asFile
        )
    }

    @Test
    fun customExecutableDirIsUsedInScripts() {
        val project = testMppProject {
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

        val scriptsTask = project.assertContainsTaskInstance<CreateStartScripts>("startScriptsForJvm")
        assertEquals("anotherBin", scriptsTask.executableDir)
    }

    @Test
    fun customJvmArgsAreUsedInScripts() {
        val project = testMppProject {
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

        val scriptsTask = project.assertContainsTaskInstance<CreateStartScripts>("startScriptsForJvm")
        assertEquals(listOf("-Xmx512m", "-Dfoo.bar=baz"), scriptsTask.defaultJvmOpts)
    }

    @Test
    fun creatingMultipleDistributions() {
        val project = testMppProject {
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

        val scriptsTask = project.assertContainsTaskInstance<CreateStartScripts>("startScriptsForJvm")
        assertEquals(project.name, scriptsTask.applicationName)
        project.assertContainsTaskInstance<Sync>("installJvmDist")
        project.assertContainsTaskInstance<Zip>("jvmDistZip")
        project.assertContainsTaskInstance<Tar>("jvmDistTar")

        val scriptsTaskMainFoo = project.assertContainsTaskInstance<CreateStartScripts>("startScriptsForJvmFoo")
        assertEquals("foo", scriptsTaskMainFoo.applicationName)
        project.assertContainsTaskInstance<Sync>("installJvmFooDist")
        project.assertContainsTaskInstance<Zip>("jvmFooDistZip")
        project.assertContainsTaskInstance<Tar>("jvmFooDistTar")

        val scriptsTaskTest = project.assertContainsTaskInstance<CreateStartScripts>("startScriptsForJvmTest")
        assertEquals(project.name, scriptsTaskTest.applicationName)
        project.assertContainsTaskInstance<Sync>("installJvmTestDist")
        project.assertContainsTaskInstance<Zip>("jvmTestDistZip")
        project.assertContainsTaskInstance<Tar>("jvmTestDistTar")
    }

    @Test
    fun createRunTaskForTestCompilation() {
        val project = testMppProject {
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

        val runTask = project.assertContainsTaskInstance<JavaExec>("runJvmTest")
        assertEquals("foo.MainKt", runTask.mainClass.get())
        assertEquals(true, runTask.javaLauncher.get().metadata.isCurrentJvm)
        val testCompilation = project.multiplatformExtension.jvm().compilations
            .getByName(KotlinCompilation.TEST_COMPILATION_NAME) as KotlinJvmCompilation
        runTask.assertClasspathContains(testCompilation.output.allOutputs)
        runTask.assertClasspathContains(testCompilation.runtimeDependencyFiles)
        assertNotNull(project.tasks.findByName("jvmTestJar"))
    }

    @Test
    fun createRunTaskForTestSuite() {
        val project = testMppProject {
            plugins.apply("jvm-test-suite")

            kotlin {
                jvm {
                    compilations.create("integrationTest")
                    binaries {
                        executable("integrationTest") {
                            mainClass.set("foo.MainKt")
                        }
                    }
                }
            }

            testing {
                suites.register("integrationTest", JvmTestSuite::class.java) {
                    it.dependencies {
                        it.implementation.add(it.project())
                    }
                }
            }
        }

        project.evaluate()

        val runTask = project.assertContainsTaskInstance<JavaExec>("runJvmIntegrationTest")
        assertEquals("foo.MainKt", runTask.mainClass.get())
        assertEquals(true, runTask.javaLauncher.get().metadata.isCurrentJvm)
        val testCompilation = project.multiplatformExtension.jvm().compilations
            .getByName("integrationTest") as KotlinJvmCompilation
        runTask.assertClasspathContains(testCompilation.output.allOutputs)
        runTask.assertClasspathContains(testCompilation.runtimeDependencyFiles)
        assertNotNull(project.tasks.findByName("jvmIntegrationTestJar"))
    }

    @Test
    fun createsDistributionForTests() {
        val project = testMppProject {
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

        val scriptsTask = project.assertContainsTaskInstance<CreateStartScripts>("startScriptsForJvmTest")
        val installTask = project.assertContainsTaskInstance<Sync>("installJvmTestDist")
        val zipTask = project.assertContainsTaskInstance<Zip>("jvmTestDistZip")
        val tarTask = project.assertContainsTaskInstance<Tar>("jvmTestDistTar")

        val testJvmCompilation = project.multiplatformExtension
            .jvm()
            .compilations
            .getByName(KotlinCompilation.TEST_COMPILATION_NAME) as KotlinJvmCompilation
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
            assertEquals(project.projectDir.resolve("build/jvmTest/scripts"), outputDir)
            assertNotNull(classpath)
            assertClasspathContains(
                project.tasks.getByName(testJvmCompilation.archiveTaskName!!).outputs.files
            )
            assertClasspathContains(testJvmCompilation.runtimeDependencyFiles.onlyJars)
            assertClasspathContains(
                project.tasks.getByName(mainJvmCompilation.archiveTaskName!!).outputs.files
            )
            assertClasspathContains(mainJvmCompilation.runtimeDependencyFiles.onlyJars)
        }

        with(installTask) {
            assertEquals(project.projectDir.resolve("build/install/${project.name}-jvmTest"), destinationDir)
        }

        with(zipTask) {
            assertEquals(
                project.projectDir.resolve("build/distributions/${project.name}-jvmTest.zip"),
                archiveFile.get().asFile
            )
        }

        with(tarTask) {
            assertEquals(
                project.projectDir.resolve("build/distributions/${project.name}-jvmTest.tar"),
                archiveFile.get().asFile
            )
        }
    }

    @Test
    fun createRunTaskForCustomCompilation() {
        val project = testMppProject {
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

        val runTask = project.assertContainsTaskInstance<JavaExec>("runJvmCustom")
        assertEquals("foo.MainKt", runTask.mainClass.get())
        assertEquals(true, runTask.javaLauncher.get().metadata.isCurrentJvm)
        assertNotNull(project.tasks.findByName("jvmCustomJar"))
    }

    @Test
    fun configuredToolchainIsAppliedToRunTask() {
        // On Windows toolchain detection is not working correctly in the functional tests
        Assume.assumeTrue(!SystemUtils.IS_OS_WINDOWS)

        val project = testMppProject {
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

        val runTask = project.assertContainsTaskInstance<JavaExec>("runJvm")
        assertEquals("21", runTask.javaLauncher.get().metadata.jvmVersion.substringBefore('.'))
    }

    @Test
    fun configureJPMSCorrectly() {
        val project = testMppProject {
            kotlin {
                jvm {
                    @Suppress("DEPRECATION")
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

        val runTask = project.assertContainsTaskInstance<JavaExec>("runJvm")
        assertEquals("foo.MainKt", runTask.mainClass.get())
        assertEquals("foo.main", runTask.mainModule.get())
        assertEquals(true, runTask.modularity.inferModulePath.get())
        assertEquals(true, runTask.javaLauncher.get().metadata.isCurrentJvm)
    }

    @Test
    fun possibleToConfigureSeveralBinariesForTheSameCompilation() {
        val project = testMppProject {
            kotlin {
                jvm {
                    binaries {
                        executable {
                            mainClass.set("foo.MainKt")
                        }

                        executable(KotlinCompilation.MAIN_COMPILATION_NAME, "another") {
                            mainClass.set("foo.MainAnotherKt")
                        }
                    }
                }
            }
        }

        project.evaluate()

        val runTask = project.assertContainsTaskInstance<JavaExec>("runJvm")
        assertEquals("foo.MainKt", runTask.mainClass.get())

        val runTaskAnother = project.assertContainsTaskInstance<JavaExec>("runJvmAnother")
        assertEquals("foo.MainAnotherKt", runTaskAnother.mainClass.get())
    }

    @Test
    fun gradleDistributionPluginShouldNotBeAppliedByDefault() {
        val project = testMppProject {
            kotlin { jvm() }
        }

        project.evaluate()

        assertFalse(project.plugins.hasPlugin("distribution"), "Gradle 'distribution' plugin should not be applied by default")
    }

    @Test
    fun possibleToConfigureSeveralBinariesForTheDifferentCompilations() {
        val project = testMppProject {
            kotlin {
                jvm {
                    binaries {
                        executable {
                            mainClass.set("foo.MainKt")
                        }

                        executable(KotlinCompilation.MAIN_COMPILATION_NAME, "another") {
                            mainClass.set("foo.MainAnotherKt")
                        }

                        executable(KotlinCompilation.TEST_COMPILATION_NAME) {
                            mainClass.set("foo.MainTestKt")
                        }

                        executable(KotlinCompilation.TEST_COMPILATION_NAME, "another") {
                            mainClass.set("foo.MainAnotherTestKt")
                        }
                    }
                }
            }
        }

        project.evaluate()

        val runTask = project.assertContainsTaskInstance<JavaExec>("runJvm")
        assertEquals("foo.MainKt", runTask.mainClass.get())

        val runTaskAnother = project.assertContainsTaskInstance<JavaExec>("runJvmAnother")
        assertEquals("foo.MainAnotherKt", runTaskAnother.mainClass.get())

        val runTaskTest = project.assertContainsTaskInstance<JavaExec>("runJvmTest")
        assertEquals("foo.MainTestKt", runTaskTest.mainClass.get())

        val runTaskTestAnother = project.assertContainsTaskInstance<JavaExec>("runJvmTestAnother")
        assertEquals("foo.MainAnotherTestKt", runTaskTestAnother.mainClass.get())
    }

    @Test
    fun testMultiproject() {
        val rootProject = buildProject(
            projectBuilder = {
                withName("root")
            }
        )

        val libProject = buildProjectWithJvm(
            projectBuilder = {
                withName("lib")
                withParent(rootProject)
            },
            preApplyCode = {
                enableDefaultStdlibDependency(false)
            },
        )

        val consumerProject = testMppProject(
            projectBuilder = {
                withName("consumer")
                withParent(rootProject)
            }
        ) {
            kotlin {
                jvm {
                    compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME).dependencies {
                        implementation(project(":lib"))
                    }
                    binaries {
                        executable(KotlinCompilation.TEST_COMPILATION_NAME) {
                            mainClass.set("foo.MainKt")
                        }
                    }
                }
            }
        }

        rootProject.evaluate()

        val libJarTask = libProject.assertContainsTaskInstance<Jar>("jar")
        val scriptsTask = consumerProject.assertContainsTaskInstance<CreateStartScripts>("startScriptsForJvmTest")
        val installTask = consumerProject.assertContainsTaskInstance<Sync>("installJvmTestDist")

        scriptsTask.assertDependsOn(libJarTask)
        scriptsTask.classpath!!.files.map { it.name }.contains(libJarTask.archiveFileName.get())

        installTask.assertDependsOn(libJarTask)
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

    private fun testMppProject(
        projectBuilder: ProjectBuilder.() -> Unit = {},
        code: Project.() -> Unit
    ) = buildProjectWithMPP(
        projectBuilder = projectBuilder,
        preApplyCode = {
            enableDefaultStdlibDependency(false)
        },
        code = code,
    )
}
