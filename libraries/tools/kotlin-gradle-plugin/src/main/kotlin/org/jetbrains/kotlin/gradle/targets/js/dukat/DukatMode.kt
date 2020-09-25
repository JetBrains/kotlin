/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

enum class DukatMode {
    SOURCE,
    BINARY;

    companion object {
        fun byArgumentOrNull(argument: String): DukatMode? =
            values().firstOrNull { it.name.equals(argument, ignoreCase = true) }
    }
}