/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

/**
 * Creates a module map for a framework.
 *
 * Reference: https://clang.llvm.org/docs/Modules.html
 */
class ModuleMapBuilder {
    fun build(frameworkName: String, moduleDependencies: Set<String>): String = buildString {
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