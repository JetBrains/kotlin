/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Named
import java.io.Serializable

enum class JsCompilerType : Named, Serializable {
    LEGACY,
    KLIB,
    BOTH;

    override fun getName(): String =
        name.toLowerCase()

    override fun toString(): String =
        getName()

    companion object {
        const val jsCompilerProperty = "kotlin.js.compiler"

        fun byArgument(argument: String): JsCompilerType? =
            JsCompilerType
                .values().firstOrNull { it.name.equals(argument, ignoreCase = true) }
    }
}