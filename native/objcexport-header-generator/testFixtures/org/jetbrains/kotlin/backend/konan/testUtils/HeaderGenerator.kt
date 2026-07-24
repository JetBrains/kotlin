/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCHeader
import org.jetbrains.kotlin.konan.test.Library
import java.io.File

interface HeaderGenerator {

    data class Configuration(
        val frameworkName: String = "",

        /**
         * Base declaration stubs do not change and have dedicated tests.
         * We do not generate them by default to keep test data easier to read.
         */
        val withObjCBaseDeclarationStubs: Boolean = false,

        /**
         * List of libraries that can be used as dependency for the compiler when generating
         * the header for the given source files.
         *
         * Some of those dependencies can also be exported, see [exportedDependencies]
         */
        val dependencies: List<Library> = listOf(),

        /**
         * Any dependency listed in [dependencies] which module name is present in this set is considered 'exported' and
         * will result in the entire public API surface of the said library to be translated in the header
         */
        val exportedDependencies: Set<Library> = emptySet(),

        val explicitMethodFamily: Boolean = false,

        val objcExportBlockExplicitParameterNames: Boolean = false,
    )


    fun generateHeaders(root: File, configuration: Configuration = Configuration()): ObjCHeader
}