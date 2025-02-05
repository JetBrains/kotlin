/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import java.io.File

internal fun createModuleMap(moduleName: String, directory: File, umbrellaHeader: File): File {
    return directory.resolve("module.modulemap").apply {
        writeText("""
            module $moduleName {
                umbrella header "${umbrellaHeader.absolutePath}"
                export *
            }
            """.trimIndent()
        )
    }
}