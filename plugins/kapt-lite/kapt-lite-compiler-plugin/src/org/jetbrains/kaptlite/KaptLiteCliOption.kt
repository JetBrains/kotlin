/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption

enum class KaptLiteCliOption(
    override val optionName: String,
    override val valueDescription: String,
    override val description: String,
    override val allowMultipleOccurrences: Boolean = false
) : AbstractCliOption {
    STUBS_OUTPUT_DIR("stubs", "<path>", "Output path for kapt stubs");

    override val required: Boolean = false
}