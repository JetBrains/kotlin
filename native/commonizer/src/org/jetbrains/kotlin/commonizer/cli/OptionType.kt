/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

internal abstract class OptionType<T>(
    val alias: String,
    val description: String,
    val mandatory: Boolean = true
) {
    abstract fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<T>
}
