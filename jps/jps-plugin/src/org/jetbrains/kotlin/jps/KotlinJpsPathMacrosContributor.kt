/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps

import org.jetbrains.jps.model.serialization.JpsPathMacroContributor

class KotlinJpsPathMacrosContributor : JpsPathMacroContributor {
    override fun getPathMacros(): Map<String, String> =
        System.getProperty("jps.kotlin.home")
            ?.let { mapOf("KOTLIN_BUNDLED" to it) }
            ?: emptyMap()
}
