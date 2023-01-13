/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.dukat

import java.io.File
import kotlin.text.Regex

fun main() {
    val outputDirectory = "../../stdlib/wasm/src/org.w3c/"
    launch(
        outputDirectory = outputDirectory,
        dynamicAsType = true,
        useStaticGetters = true
    )
    File(outputDirectory).walk().forEach {
        if (it.isFile && it.name.endsWith(".kt")) {
            it.writeText(postProcessIdlBindings(it.readText()))
        }
    }
}

fun postProcessIdlBindings(source: String): String {
    return source
        .replace(
            Regex("( {4}return o as \\w+)"),
            "    @Suppress(\"UNCHECKED_CAST_TO_EXTERNAL_INTERFACE\")\n\$1"
        )
}