/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill

import java.io.File
import org.gradle.api.Project

open class PillExtensionMirror(variant: String, val excludedDirs: List<File>) {
    val variant = Variant.valueOf(variant)

    enum class Variant {
        // Default variant (./gradlew pill)
        BASE() {
            override val includes = setOf(BASE)
        },

        // Full variant (./gradlew pill -Dpill.variant=full)
        FULL() {
            override val includes = setOf(BASE, FULL)
        },

        // Do not import the project to JPS model, but set some options for it
        NONE() {
            override val includes = emptySet<Variant>()
        },

        // 'BASE' if the "jps-compatible" plugin is applied, 'NONE' otherwise
        DEFAULT() {
            override val includes = emptySet<Variant>()
        };

        abstract val includes: Set<Variant>
    }
}

fun Project.findPillExtensionMirror(): PillExtensionMirror? {
    val ext = extensions.findByName("pill") ?: return null
    @Suppress("UNCHECKED_CAST")
    val serialized = ext::class.java.getMethod("serialize").invoke(ext) as Map<String, Any>

    val variant = serialized["variant"] as String

    @Suppress("UNCHECKED_CAST")
    val excludedDirs = serialized["excludedDirs"] as List<File>

    val constructor = PillExtensionMirror::class.java.declaredConstructors.single()
    return constructor.newInstance(variant, excludedDirs) as PillExtensionMirror
}