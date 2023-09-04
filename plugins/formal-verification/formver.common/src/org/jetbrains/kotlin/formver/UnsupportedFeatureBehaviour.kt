/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver


enum class UnsupportedFeatureBehaviour {
    THROW_EXCEPTION,
    ASSUME_UNREACHABLE;

    companion object {
        @JvmStatic
        fun defaultBehaviour(): UnsupportedFeatureBehaviour {
            return THROW_EXCEPTION
        }
    }
}
