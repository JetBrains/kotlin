/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import java.nio.file.Files
import java.nio.file.Path

/**
 * Modify file content under [Path].
 *
 * @param transform function receiving current file content and outputting new file content
 */
fun Path.modify(transform: (currentContent: String) -> String) {
    assert(Files.isRegularFile(this)) { "$this is not a regular file!" }

    val file = toFile()
    file.writeText(transform(file.readText()))
}

/**
 * Append [textToAppend] to the file content under [Path].
 */
fun Path.append(
    textToAppend: String
) {
    modify {
        """
            $it
            
            $textToAppend
        """.trimIndent()
    }
}
