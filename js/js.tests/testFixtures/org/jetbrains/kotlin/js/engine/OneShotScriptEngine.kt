/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.engine

import java.io.File

internal sealed class OneShotScriptEngine(propertyPath: String) {
    protected val tool = ExternalTool(System.getProperty(propertyPath))

    abstract fun run(jsFiles: List<String>, workingDirectory: File?, toolArgs: List<String> = emptyList()): String

    object V8 : OneShotScriptEngine("javascript.engine.path.V8") {
        override fun run(jsFiles: List<String>, workingDirectory: File?, toolArgs: List<String>) =
            tool.run(*toolArgs.toTypedArray(), *jsFiles.toTypedArray(), workingDirectory = workingDirectory)
    }
}
