/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import java.io.File

fun Appendable.appendConfigsFromDir(confDir: File) {
    if (!confDir.isDirectory) return

    confDir.listFiles()
        ?.toList()
        ?.filter { it.name.endsWith(".js") }
        ?.forEach {
            appendln("// ${it.name}")
            append(it.readText())
            appendln()
            appendln()
        }
}
