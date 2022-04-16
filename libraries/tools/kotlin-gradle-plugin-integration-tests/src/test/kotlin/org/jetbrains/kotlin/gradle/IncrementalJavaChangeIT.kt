/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.assertTrue

@DisplayName("Default incremental compilation with default precise java tracking")
open class IncrementalJavaChangeDefaultIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = null) {

    @DisplayName("Lib: tracked method signature ABI change")
    @GradleTest
    override fun testAbiChangeInLib_changeMethodSignature_tracked(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            trackedJavaClassInLib.modify(changeMethodSignature)

            build("assemble") {
                val expectedSources = sourceFilesRelativeToProject(
                    listOf("foo/TrackedJavaClassChild.kt", "foo/useTrackedJavaClass.kt"),
                    subProjectName = "app"
                )
                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }

    @DisplayName("Lib: tracked method body non-ABI change")
    @GradleTest
    override fun testNonAbiChangeInLib_changeMethodBody_tracked(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            trackedJavaClassInLib.modify(changeMethodBody)

            build("assemble") {
                assertCompiledKotlinSources(emptyList(), output)
            }
        }
    }

    @DisplayName("KT-38692: should clean all outputs after removing all Kotlin sources")
    @GradleTest
    fun testIncrementalWhenNoKotlinSources(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            build(":compileKotlin") {
                assertTasksExecuted(":compileKotlin")
            }

            // Remove all Kotlin sources and force non-incremental run
            projectPath.allKotlinFiles.forEach { it.deleteExisting() }
            javaSourcesDir().resolve("Sample.java").also {
                it.parent.createDirectories()
                it.writeText("public class Sample {}")
            }
            build("compileKotlin", "--rerun-tasks") {
                assertTasksExecuted(":compileKotlin")
                assertTrue(kotlinClassesDir().notExists())
            }
        }
    }

    @DisplayName("Type alias change is incremental")
    @GradleTest
    fun testTypeAliasIncremental(gradleVersion: GradleVersion) {
        project("typeAlias", gradleVersion) {
            build("build")

            val curryKt = kotlinSourcesDir().resolve("Curry.kt")
            val useCurryKt = kotlinSourcesDir().resolve("UseCurry.kt")

            curryKt.modify {
                it.replace("class Curry", "internal class Curry")
            }

            build("build") {
                assertCompiledKotlinSources(
                    listOf(curryKt, useCurryKt).map { it.relativeTo(projectPath) },
                    output
                )
            }
        }
    }
}

@DisplayName("Incremental compilation via classpath snapshots with default precise java tracking")
class IncrementalJavaChangeClasspathSnapshotIT : IncrementalJavaChangeDefaultIT() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(useGradleClasspathSnapshot = true)

    @DisplayName("Lib: tracked method signature ABI change")
    @GradleTest
    override fun testAbiChangeInLib_changeMethodSignature(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            javaClassInLib.modify(changeMethodSignature)

            build("assemble") {
                // Fewer Kotlin files are recompiled
                val expectedSources = sourceFilesRelativeToProject(
                    listOf("foo/JavaClassChild.kt", "foo/useJavaClass.kt"),
                    subProjectName = "app"
                )
                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }

    @DisplayName("Lib: method body non-ABI change")
    @GradleTest
    override fun testNonAbiChangeInLib_changeMethodBody(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            javaClassInLib.modify(changeMethodBody)

            build("assemble") {
                assertTasksExecuted(":lib:compileKotlin")
                assertTasksUpToDate(":app:compileKotlin") // App compilation has 'compile avoidance'
                assertCompiledKotlinSources(emptyList(), output)
            }
        }
    }
}

@DisplayName("Default incremental compilation with enabled precise java tracking")
class IncrementalJavaChangePreciseIT : IncrementalCompilationJavaChangesBase(
    usePreciseJavaTracking = true
) {
    @DisplayName("Lib: tracked method signature ABI change")
    @GradleTest
    override fun testAbiChangeInLib_changeMethodSignature_tracked(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            trackedJavaClassInLib.modify(changeMethodSignature)

            build("assemble") {
                val expectedSources = sourceFilesRelativeToProject(
                    listOf("foo/TrackedJavaClassChild.kt", "foo/useTrackedJavaClass.kt"),
                    subProjectName = "app"
                )
                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }

    @DisplayName("Lib: tracked method body non-ABI change")
    @GradleTest
    override fun testNonAbiChangeInLib_changeMethodBody_tracked(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            trackedJavaClassInLib.modify(changeMethodBody)

            build("assemble") {
                assertCompiledKotlinSources(emptyList(), output)
            }
        }
    }
}

@DisplayName("Default incremental compilation with disabled precise java tracking")
open class IncrementalJavaChangeDisablePreciseIT : IncrementalCompilationJavaChangesBase(
    usePreciseJavaTracking = false
) {
    @DisplayName("Lib: tracked method signature ABI change")
    @GradleTest
    override fun testAbiChangeInLib_changeMethodSignature_tracked(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            trackedJavaClassInLib.modify(changeMethodSignature)

            build("assemble") {
                val expectedSources = sourceFilesRelativeToProject(
                    listOf(
                        "foo/TrackedJavaClassChild.kt",
                        "foo/useTrackedJavaClass.kt",
                        "foo/useTrackedJavaClassFooMethodUsage.kt"
                    ),
                    subProjectName = "app"
                ) + sourceFilesRelativeToProject(
                    listOf("bar/useTrackedJavaClassSameModule.kt"),
                    subProjectName = "lib"
                )
                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }

    @DisplayName("Lib: tracked method body non-ABI change")
    @GradleTest
    override fun testNonAbiChangeInLib_changeMethodBody_tracked(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            trackedJavaClassInLib.modify(changeMethodBody)

            build("assemble") {
                val expectedSources = sourceFilesRelativeToProject(
                    listOf(
                        "foo/TrackedJavaClassChild.kt",
                        "foo/useTrackedJavaClass.kt",
                        "foo/useTrackedJavaClassFooMethodUsage.kt"
                    ),
                    subProjectName = "app"
                ) + sourceFilesRelativeToProject(
                    listOf("bar/useTrackedJavaClassSameModule.kt"),
                    subProjectName = "lib"
                )
                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }
}

@DisplayName("Default incremental compilation with disabled precise java tracking and enabled FIR")
class IncrementalFirJavaChangeDisablePreciseIT : IncrementalJavaChangeDisablePreciseIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(useFir = true)
}

@JvmGradlePluginTests
abstract class IncrementalCompilationJavaChangesBase(
    val usePreciseJavaTracking: Boolean?
) : IncrementalCompilationBaseIT() {
    override val defaultProjectName: String
        get() = "incrementalMultiproject"

    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseJavaTracking = usePreciseJavaTracking)

    protected val TestProject.javaClassInLib: Path get() = subProject("lib").javaSourcesDir().resolve("bar/JavaClass.java")
    protected val TestProject.trackedJavaClassInLib: Path get() = subProject("lib").javaSourcesDir().resolve("bar/TrackedJavaClass.java")
    protected val changeMethodSignature: (String) -> String = { it.replace("String getString", "Object getString") }
    protected val changeMethodBody: (String) -> String = { it.replace("Hello, World!", "Hello, World!!!!") }

    @DisplayName("Lib: method signature ABI change")
    @GradleTest
    open fun testAbiChangeInLib_changeMethodSignature(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            javaClassInLib.modify(changeMethodSignature)

            build("assemble") {
                val expectedToCompileSources = sourceFilesRelativeToProject(
                    listOf(
                        "foo/JavaClassChild.kt",
                        "foo/useJavaClass.kt",
                        "foo/useJavaClassFooMethodUsage.kt"
                    ),
                    subProjectName = "app"
                )
                assertCompiledKotlinSources(
                    expectedToCompileSources,
                    output
                )
            }
        }
    }

    @DisplayName("Lib: method body non-ABI change")
    @GradleTest
    open fun testNonAbiChangeInLib_changeMethodBody(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            javaClassInLib.modify(changeMethodBody)

            build("assemble") {
                val expectedToCompileSources = sourceFilesRelativeToProject(
                    listOf(
                        "foo/JavaClassChild.kt",
                        "foo/useJavaClass.kt",
                        "foo/useJavaClassFooMethodUsage.kt"
                    ),
                    subProjectName = "app"
                )

                assertCompiledKotlinSources(
                    expectedToCompileSources,
                    output
                )
            }
        }
    }

    abstract fun testAbiChangeInLib_changeMethodSignature_tracked(gradleVersion: GradleVersion)
    abstract fun testNonAbiChangeInLib_changeMethodBody_tracked(gradleVersion: GradleVersion)
}
