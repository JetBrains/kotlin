/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.dukat

import org.jetbrains.dukat.translatorString.compileUnits
import org.jetbrains.dukat.translatorString.translateSourceSet
import org.jetbrains.kotlin.tools.dukat.wasm.translateIdlToSourceSet
import java.io.File

fun main() {
    val outputDirectory = "../../stdlib/wasm/src/org.w3c/"
    val input = "../../stdlib/js/idl/org.w3c.dom.idl"

    val sourceSet = translateIdlToSourceSet(input)
    compileUnits(translateSourceSet(sourceSet), outputDirectory)

    File(outputDirectory).walk().forEach {
        if (it.isFile && it.name.endsWith(".kt")) {
            it.writeText(getHeader() + it.readText())
        }
    }
}