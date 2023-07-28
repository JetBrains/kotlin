/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill

import java.io.File
import org.gradle.api.Project

open class PillExtensionMirror(variant: String?, val excludedDirs: List<File>) {
    val variant = if (variant == null) null else Variant.valueOf(variant)

    enum class Variant(includesFactory: () -> Set<Variant>) {
        BASE({ setOf(BASE) }), // Includes compiler and IDE (default)
        FULL({ setOf(BASE, FULL) }); // Includes compiler, IDE and Gradle plugin

        val includes by lazy { includesFactory() }
    }
}

fun Project.findPillExtensionMirror(): PillExtensionMirror? {
    val ext = extensions.findByName("pill") ?: return null

    @Suppress("UNCHECKED_CAST")
    val serialized = ext::class.java.getMethod("serialize").invoke(ext) as Map<String, Any>

    val variant = serialized["variant"] as String?

    @Suppress("UNCHECKED_CAST")
    val excludedDirs = serialized["excludedDirs"] as List<File>

    return PillExtensionMirror(variant, excludedDirs)
}