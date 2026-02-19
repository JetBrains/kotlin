/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.file.CopySpec
import org.jetbrains.kotlin.gradle.targets.js.internal.RewriteSourceMapFilterReader
import java.io.File

internal fun CopySpec.remapJavaScriptSourceMapSourcePaths(destinationDir: File) {
    eachFile {
        if (it.name.endsWith(".js.map") || it.name.endsWith(".mjs.map")) {
            it.filter(
                mapOf(
                    RewriteSourceMapFilterReader::srcSourceRoot.name to it.file.parentFile,
                    RewriteSourceMapFilterReader::targetSourceRoot.name to destinationDir
                ),
                RewriteSourceMapFilterReader::class.java
            )
        }
    }
}