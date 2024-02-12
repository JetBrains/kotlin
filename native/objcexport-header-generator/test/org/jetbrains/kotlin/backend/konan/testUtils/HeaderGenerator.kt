/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCHeader
import java.io.File

interface HeaderGenerator {

    data class Configuration(
        val frameworkName: String = "",

        /**
         * Base declaration stubs do not change and have dedicated tests.
         * We do not generate them by default to keep test data easier to read.
         */
        val generateBaseDeclarationStubs: Boolean = false,
    )

    fun generateHeaders(root: File, configuration: Configuration = Configuration()): ObjCHeader
}