/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

enum class ExternalsOutputFormat {
    SOURCE,
    BINARY;

    companion object {
        fun byArgumentOrNull(argument: String): ExternalsOutputFormat? =
            values().firstOrNull { it.name.equals(argument, ignoreCase = true) }

        const val externalsOutputFormatProperty = "kotlin.js.externals.output.format"
    }
}