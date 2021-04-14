/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.utils.appendLine
import java.io.File

fun Appendable.appendConfigsFromDir(confDir: File) {
    val files = confDir.listFiles() ?: return

    files.asSequence()
        .filter { it.isFile }
        .filter { it.extension == "js" }
        .sortedBy { it.name }
        .forEach {
            appendLine("// ${it.name}")
            append(it.readText())
            appendLine()
            appendLine()
        }
}


const val JS = "js"
const val JS_MAP = "js.map"
const val META_JS = "meta.js"
