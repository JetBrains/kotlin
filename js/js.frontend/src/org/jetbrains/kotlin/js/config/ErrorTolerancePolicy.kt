/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.config

enum class ErrorTolerancePolicy {
    NONE;

    val allowSemanticErrors = false
    val allowSyntaxErrors = false
    val allowErrors = false

    companion object {
        val DEFAULT = NONE

        fun resolvePolicy(key: String): ErrorTolerancePolicy {
            return when (key.uppercase()) {
                "NONE" -> NONE
                else -> error("KT-65018: -Xerror-tolerance-policy is deprecated and will be removed in next compiler version. " +
                                      "Only `NONE` value is allowed now.")
            }
        }
    }
}
