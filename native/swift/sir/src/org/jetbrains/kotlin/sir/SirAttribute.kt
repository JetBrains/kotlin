/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

public sealed interface SirAttribute {
    val identifier: String
    val arguments: List<SirArgument>? get() = null

    /**
     * Loosely models @available attribute
     * https://docs.swift.org/swift-book/documentation/the-swift-programming-language/attributes/#available
     *
     */
    class Available(
        val message: String?,
        val deprecated: Boolean = false,
        val obsoleted: Boolean = false,
        val unavailable: Boolean = false,
        val renamed: String = ""
    ) : SirAttribute {
        init {
            require(obsoleted || deprecated || unavailable) { "Positive availability is not supported" }
            require((obsoleted || deprecated) != unavailable) { "Declaration can not be both deprecated/obsolete and unavailable" }
        }

        override val identifier: String get() = "available"

        override val arguments: List<SirArgument> get() = listOfNotNull(
                SirArgument("*"),
                SirArgument("deprecated").takeIf { deprecated && !unavailable },
                SirArgument("obsoleted", "1.0").takeIf { obsoleted && !unavailable },
                SirArgument("unavailable").takeIf { unavailable },
                renamed.takeIf { it.isNotEmpty() }?.let { SirArgument("renamed", SirExpression.StringLiteral(renamed)) },
                message?.let { SirArgument("message", SirExpression.StringLiteral(message)) },
            )
    }

    object NonOverride : SirAttribute {
        override val identifier: String get() = "_nonoverride"

        override val arguments: List<SirArgument>? get() = null
    }
}