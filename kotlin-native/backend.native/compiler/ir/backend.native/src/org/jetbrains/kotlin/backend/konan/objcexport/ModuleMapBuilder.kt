/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

class ModuleMapBuilder(
        private val frameworkName: String
) {
    fun build(): String {
        val moduleMap = """
            |framework module $frameworkName {
            |    umbrella header "$frameworkName.h"
            |
            |    export *
            |    module * { export * }
            |
            |    use Foundation
            |}
        """.trimMargin()
        return moduleMap
    }
}