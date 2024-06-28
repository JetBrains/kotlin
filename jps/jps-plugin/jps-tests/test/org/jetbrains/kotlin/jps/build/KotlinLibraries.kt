/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.java.JpsJavaLibraryType
import org.jetbrains.jps.model.library.JpsLibrary
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.kotlin.test.kotlinPathsForDistDirectoryForTests
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

enum class KotlinJpsLibrary(val id: String, roots: () -> Array<File>) {
    MockRuntime(
        "kotlin-mock-runtime",
        {
            arrayOf(
                PathUtil.kotlinPathsForDistDirectoryForTests.stdlibPath,
                File(PathUtil.kotlinPathsForDistDirectoryForTests.libPath, "annotations-13.0.jar"),
            )
        }
    ),

    JvmStdLib(
        "kotlin-stdlib",
        {
            arrayOf(
                PathUtil.kotlinPathsForDistDirectoryForTests.stdlibPath,
                File(PathUtil.kotlinPathsForDistDirectoryForTests.libPath, "annotations-13.0.jar"),
            )
        }
    ),

    JvmTest(
        "kotlin-test",
        { arrayOf(PathUtil.kotlinPathsForDistDirectoryForTests.kotlinTestPath) },
    ),

    JsStdLib(
        "KotlinJavaScript",
        { arrayOf(PathUtil.kotlinPathsForDistDirectoryForTests.jsStdLibKlibPath) }
    ),
    JsTest(
        "KotlinJavaScriptTest",
        { arrayOf(PathUtil.kotlinPathsForDistDirectoryForTests.jsKotlinTestKlibPath) }
    ),
    Lombok(
        "lombok",
        {
            arrayOf(
                PathUtil.kotlinPathsForDistDirectoryForTests.stdlibPath,
                File(lombok.Lombok::class.java.protectionDomain.codeSource.location.toURI().path),
            )
        }
    );

    val roots: Array<File> by lazy(roots)

    fun create(project: JpsProject): JpsLibrary {
        val library = project.addLibrary(id, JpsJavaLibraryType.INSTANCE)

        for (fileRoot in roots) {
            library.addRoot(fileRoot, JpsOrderRootType.COMPILED)
        }

        return library
    }
}