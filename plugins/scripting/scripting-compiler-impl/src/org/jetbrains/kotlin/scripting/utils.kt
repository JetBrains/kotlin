/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting

import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.fileExtension

fun SourceCode.scriptFileName(
    mainScript: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration
): String {
    val mainExtension = scriptCompilationConfiguration[ScriptCompilationConfiguration.fileExtension]
    return when {
        name != null -> withCorrectExtension(name!!, mainExtension)
        mainScript == this -> withCorrectExtension("script", mainExtension)
        else -> throw Exception("Unexpected script without name: $this")
    }
}

internal fun withCorrectExtension(name: String, mainExtension: String?): String =
    // TODO: consider checking for all registered extensions
    if ((mainExtension != null && name.endsWith(".$mainExtension")) || name.endsWith(".kts")) name else "$name.${mainExtension ?: "kts"}"

