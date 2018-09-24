/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.KonanTarget

interface SearchPathResolver {
    val searchRoots: List<File>
    fun resolve(givenPath: String): File
    fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean): List<File>
}

interface SearchPathResolverWithTarget : SearchPathResolver {
    val target: KonanTarget
}
