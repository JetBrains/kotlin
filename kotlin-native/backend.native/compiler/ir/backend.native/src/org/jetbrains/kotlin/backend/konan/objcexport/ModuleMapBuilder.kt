/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

class ModuleMapBuilder(
        private val frameworkName: String,
        private val moduleDependencies: Set<String>
) {
    fun build(): String = buildString {
        appendLine("framework module $frameworkName {")
        appendLine("    umbrella header \"$frameworkName.h\"")
        appendLine()
        appendLine("    export *")
        appendLine("    module * { export * }")
        appendLine()
        moduleDependencies.forEach {
            appendLine("    use $it")
        }
        appendLine("}")
    }
}