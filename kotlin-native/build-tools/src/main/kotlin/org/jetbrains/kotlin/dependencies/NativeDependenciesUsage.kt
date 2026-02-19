/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dependencies

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage

object NativeDependenciesUsage {
    /**
     * Directories with native dependencies.
     */
    @JvmField
    val NATIVE_DEPENDENCY = "native-dependency"

    @JvmField
    val USAGE_ATTRIBUTE: Attribute<Usage> = Usage.USAGE_ATTRIBUTE
}