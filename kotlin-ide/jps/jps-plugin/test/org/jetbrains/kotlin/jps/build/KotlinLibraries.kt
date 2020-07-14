/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.java.JpsJavaLibraryType
import org.jetbrains.jps.model.library.JpsLibrary
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import java.io.File

enum class KotlinJpsLibrary(val id: String, vararg val roots: File) {
    @Deprecated("Use JvmStdlib instead")
    MockRuntime(
        "kotlin-mock-runtime",
        KotlinArtifacts.kotlinStdlib,
        KotlinArtifacts.jetbrainsAnnotations
    ),

    JvmStdLib(
        "kotlin-stdlib",
        KotlinArtifacts.kotlinStdlib,
        KotlinArtifacts.jetbrainsAnnotations
    ),
    JvmTest(
        "kotlin-test",
        KotlinArtifacts.kotlinTest
    ),

    JsStdLib(
        "KotlinJavaScript",
        KotlinArtifacts.kotlinStdlibJs
    ),
    JsTest(
        "KotlinJavaScriptTest",
        KotlinArtifacts.kotlinTestJs
    );

    fun create(project: JpsProject): JpsLibrary {
        val library = project.addLibrary(id, JpsJavaLibraryType.INSTANCE)

        for (fileRoot in roots) {
            library.addRoot(fileRoot, JpsOrderRootType.COMPILED)
        }

        return library
    }
}