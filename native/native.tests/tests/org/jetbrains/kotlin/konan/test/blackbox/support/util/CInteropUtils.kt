/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

fun File.defFileIsSupportedOn(target: KonanTarget): Boolean = readText().defFileContentsIsSupportedOn(target)

fun String.defFileContentsIsSupportedOn(target: KonanTarget): Boolean {
    if (target.family.isAppleFamily) return true

    for (line in lines()) {
        if (line.startsWith("---")) return true

        val parts = line.split('=')
        if (parts.size == 2
            && parts[0].trim().equals("language", ignoreCase = true)
            && parts[1].trim().equals("Objective-C", ignoreCase = true)
        ) {
            return false
        }
    }
    return true
}