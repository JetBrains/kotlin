/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

enum class ErrorStyle {
    USER_FRIENDLY,
    ORIGINAL_VIPER,
    BOTH;

    companion object {
        @JvmStatic
        fun defaultBehaviour(): ErrorStyle = USER_FRIENDLY
    }
}