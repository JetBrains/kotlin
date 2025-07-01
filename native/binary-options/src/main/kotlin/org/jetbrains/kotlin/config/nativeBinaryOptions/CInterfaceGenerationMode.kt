/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

/**
 * Configure the way C export works for dynamic/static libraries.
 */
enum class CInterfaceGenerationMode {
    /**
     * Do not generate any C interface.
     */
    NONE,

    /**
     * Generate C header file the way it is described in
     * https://kotlinlang.org/docs/native-dynamic-libraries.html.
     *
     */
    V1
}