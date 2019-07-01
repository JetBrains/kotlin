/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.lite

import java.io.File

interface LiteKonanLibrary {
    val path: File
    val sourcePaths: Collection<File>
    val name: String
    val platform: String?
}
