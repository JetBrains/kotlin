/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.assertEquals


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
}