/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import java.io.File

/** Information to locate a .class file. */
class ClassFile(

    /** Directory or jar containing the .class file. */
    val classRoot: File,

    /**
     * The relative path from [classRoot] to the .class file.
     *
     * Any '\' characters in the path will be replaced with '/' to create [unixStyleRelativePath].
     */
    relativePath: String
) {

    /** The Unix-style relative path (with '/' as separators) from [classRoot] to the .class file. */
    val unixStyleRelativePath: String

    init {
        unixStyleRelativePath = relativePath.replace('\\', '/')
    }
}

/** Information to locate a .class file, plus their contents. */
class ClassFileWithContents(
    val classFile: ClassFile,
    val contents: ByteArray
)
