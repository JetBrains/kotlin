/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill

import java.io.File
import org.gradle.api.Project

open class PillExtensionMirror(val excludedDirs: List<File>)

fun Project.findPillExtensionMirror(): PillExtensionMirror? {
    val ext = extensions.findByName("pill") ?: return null

    @Suppress("UNCHECKED_CAST")
    val serialized = ext::class.java.getMethod("serialize").invoke(ext) as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    val excludedDirs = serialized["excludedDirs"] as List<File>

    return PillExtensionMirror(excludedDirs)
}