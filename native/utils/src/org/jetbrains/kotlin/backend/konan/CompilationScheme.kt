/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

enum class CompilationScheme {
    CLOSED,
    SPLIT_HOST;

    companion object {
        /**
         * By default, the compilation scheme is the classical one: [CLOSED].
         */
        val DEFAULT = CLOSED
    }
}
