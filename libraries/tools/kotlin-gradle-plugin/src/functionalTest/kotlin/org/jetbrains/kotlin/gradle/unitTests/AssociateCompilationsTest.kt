/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.util.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail


class AssociateCompilationsTest {

    @Test
    fun `test - associatedCompilations and allAssociatedCompilations sets`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()

        val main = kotlin.jvm().compilations.main
        val test = kotlin.jvm().compilations.test
        val bar = kotlin.jvm().compilations.create("bar")
        val foo = kotlin.jvm().compilations.create("foo")

        // Check: foo is not associated with anything
        assertEquals(emptySet(), foo.internal.associatedCompilations.toSet())
        assertEquals(emptySet(), foo.internal.allAssociatedCompilations.toSet())

        // Check: foo associates with test (which is already associated with main by default)
        foo.associateWith(test)
        assertEquals(setOf(test), foo.internal.associatedCompilations.toSet())
        assertEquals(setOf(main, test), foo.internal.allAssociatedCompilations.toSet())

        // Check: associate main with bar (should be reflected in foo transitively)
        main.associateWith(bar)
        assertEquals(setOf(test), foo.internal.associatedCompilations.toSet())
        assertEquals(setOf(main, test, bar), foo.internal.allAssociatedCompilations.toSet())
    }

    @Test
    fun `test - associate compilation dependency files are first in compile path`() {
        val project = buildProject { enableDefaultStdlibDependency(false) }
        val kotlin = project.applyMultiplatformPlugin()
        val jvm = kotlin.jvm()
        val foo = jvm.compilations.create("foo")

        foo.defaultSourceSet.dependencies {
            implementation(project.files("bar.jar"))
        }

        foo.associateWith(jvm.compilations.main)

        assertEquals(
            listOf(jvm.compilations.main.output.classesDirs, project.files("bar.jar")).flatten(),
            foo.compileDependencyFiles.files.toList(),
            "Expected 'main classesDirs' to be listed before file dependency 'bar.jar'"
        )
    }

    @Test
    fun `test - friendPaths are included in JVM compile dependencies`() =
        testCustomCompilationAssociation(
            forTarget = { jvm() },
            taskCast = { it as KotlinJvmCompile },
            friendPaths = { friendPaths.files },
            libraries = { libraries.files },
            // FIXME: KT-85773 Make JVM friend paths to be in sync with classpath
            allowedExtraFriendPaths = Regex(".*build.libs.test-jvm\\.jar")
        )

    @Test
    fun `test - friendPaths are included in Native compile dependencies`() =
        testCustomCompilationAssociation(
            forTarget = { linuxX64() },
            taskCast = { it as KotlinNativeCompile },
            friendPaths = { friendModule.files },
            libraries = { libraries.files }
        )

    @Test
    fun `test - friendPaths are included in JS compile dependencies`() =
        testCustomCompilationAssociation(
            forTarget = { js() },
            taskCast = { it as Kotlin2JsCompile },
            friendPaths = { friendPaths.files },
            libraries = { libraries.files }
        )

    @Test
    fun `test - friendPaths are included in Wasm compile dependencies`() =
        @OptIn(ExperimentalWasmDsl::class)
        testCustomCompilationAssociation(
            forTarget = { wasmWasi() },
            taskCast = { it as Kotlin2JsCompile },
            friendPaths = { friendPaths.files },
            libraries = { libraries.files }
        )

    @Test
    fun `test - friendPaths are included in Kotlin JVM compile dependencies`() {
        val project = buildProject {
            configureRepositoriesForTests()
            applyKotlinJvmPlugin()
        }
        val kotlin = project.kotlinJvmExtension

        val target = kotlin.target
        val jvmCustom = target.compilations.create("custom")
        jvmCustom.associateWith(target.compilations.getByName("main"))

        project.evaluate()

        val compileTask = jvmCustom.compileTaskProvider.get() as KotlinJvmCompile
        val libraries = compileTask.libraries.files
        val friendPaths = compileTask.friendPaths.files
        assertLibrariesContainsFriendPaths(
            libraries,
            friendPaths,
            // FIXME: KT-85773 Make JVM friend paths to be in sync with classpath
            allowed = Regex(".*build.libs.test\\.jar"))
    }

    private fun <T> testCustomCompilationAssociation(
        forTarget: KotlinMultiplatformExtension.() -> KotlinTarget,
        taskCast: (KotlinCompilationTask<*>) -> T,
        libraries: T.() -> Collection<File>,
        friendPaths: T.() -> Collection<File>,
        allowedExtraFriendPaths: Regex? = null
    ) {
        val project = buildProjectWithMPP {
            configureRepositoriesForTests()
        }
        val kotlin = project.multiplatformExtension

        val target = kotlin.forTarget()
        val jvmCustom = target.compilations.create("custom")
        jvmCustom.associateWith(target.compilations.getByName("main"))

        project.evaluate()

        val compileTask = taskCast(jvmCustom.compileTaskProvider.get())
        val libraries = compileTask.libraries()
        val friendPaths = compileTask.friendPaths()
        assertLibrariesContainsFriendPaths(libraries, friendPaths, allowedExtraFriendPaths)
    }

    private fun assertLibrariesContainsFriendPaths(
        libraries: Collection<File>,
        friendPaths: Collection<File>,
        allowed: Regex?
    ) {
        val unexpectedFriendPaths = (friendPaths - libraries).let {
            if (allowed != null) {
                it.filterNot { path -> allowed.matches(path.absolutePath) }
            } else {
                it
            }
        }

        if (unexpectedFriendPaths.isNotEmpty()) {
            fail(buildString {
                appendLine("Unexpected friendPaths: ")
                unexpectedFriendPaths.forEach { appendLine("* $it") }
                appendLine("Libraries:")
                libraries.forEach { appendLine("* $it") }
                appendLine("FriendPaths:")
                friendPaths.forEach { appendLine("* $it") }
            })
        }
    }
}
