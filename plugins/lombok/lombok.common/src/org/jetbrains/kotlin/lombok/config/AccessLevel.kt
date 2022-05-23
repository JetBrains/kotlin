/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.config

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities

enum class AccessLevel {
    PUBLIC, MODULE, PROTECTED, PACKAGE, PRIVATE,

    /** Represents not generating anything or the complete lack of a method. */
    NONE;

    fun toVisibility(): Visibility = toVisibility(this)

    companion object {
        private fun toVisibility(v: AccessLevel): Visibility {
            return when (v) {
                PUBLIC -> Visibilities.Public
                PROTECTED -> Visibilities.Protected
                PACKAGE, MODULE -> JavaVisibilities.PackageVisibility
                PRIVATE -> Visibilities.Private
                NONE -> Visibilities.Private
            }
        }
    }
}
