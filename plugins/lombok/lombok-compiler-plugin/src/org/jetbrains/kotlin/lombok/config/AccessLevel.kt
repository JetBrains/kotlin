/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.config

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities


enum class AccessLevel {
    PUBLIC, MODULE, PROTECTED, PACKAGE, PRIVATE,

    /** Represents not generating anything or the complete lack of a method. */
    NONE;

    fun toDescriptorVisibility(): DescriptorVisibility = toDescriptorVisibility(this)

    companion object {
        fun toDescriptorVisibility(v: AccessLevel): DescriptorVisibility {
            val visibility = when (v) {
                PUBLIC -> Visibilities.Public
                PROTECTED -> Visibilities.Protected
                PRIVATE -> Visibilities.Private
                PACKAGE -> JavaVisibilities.PackageVisibility
                MODULE -> Visibilities.Internal
                NONE -> Visibilities.Private
            }
            return JavaDescriptorVisibilities.toDescriptorVisibility(visibility)
        }
    }
}
