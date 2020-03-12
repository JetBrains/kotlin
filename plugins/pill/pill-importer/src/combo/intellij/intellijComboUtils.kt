/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.combo.intellij

import java.io.File

fun getUrlPath(url: String): String {
    if (url.startsWith("file://")) {
        return url.drop("file://".length)
    } else if (url.startsWith("jar://")) {
        if (url.endsWith("!/")) {
            return url.drop("jar://".length).dropLast("!/".length)
        } else {
            throw IllegalStateException("Only root JAR entries are supported, got $url")
        }
    }

    error("Unsupported URL scheme: $url")
}

fun patchPaths(content: String, projectDir: File, moduleFile: File? = null): String {
    var result = content.replace("\$PROJECT_DIR\$/", "\$PROJECT_DIR\$/" + projectDir.name + "/")

    if (moduleFile != null) {
        val modulePath = projectDir.toRelativeString(moduleFile.parentFile)
        result = result.replace("\$MODULE_DIR\$/$modulePath/", "\$MODULE_DIR\$/$modulePath/" + projectDir.name + "/")
    }

    return result
}