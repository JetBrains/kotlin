/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

public sealed interface SirAttribute {
    /**
     * Models @available attribute
     * https://docs.swift.org/swift-book/documentation/the-swift-programming-language/attributes/#available
     */
    class Available(
        val message: String,
        val deprecated: Boolean,
        val obsoleted: Boolean,
    ) : SirAttribute {

        val platform: String
            // For now, we don't care about the platform.
            get() = "*"
    }
}