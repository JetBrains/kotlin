/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

public sealed interface SirFunctionalTypeAttribute {
    fun isPrintableInPosition(position: SirTypeVariance): Boolean
}

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
        val unavailable: Boolean = false,
        val renamed: String = ""
    ) : SirAttribute {
        init {
            require(deprecated || unavailable) { "Positive availability is not supported" }
            require(deprecated != unavailable) { "Declaration can not be both deprecated and unavailable" }
        }

        override val identifier: String get() = "available"

        override val arguments: List<SirArgument> get() = listOfNotNull(
                SirArgument("*"),
                SirArgument("deprecated").takeIf { deprecated },
                SirArgument("unavailable").takeIf { unavailable },
                renamed.takeIf { it.isNotEmpty() }?.let { SirArgument("renamed", SirExpression.StringLiteral(renamed)) },
                message?.let { SirArgument("message", SirExpression.StringLiteral(message)) },
            )

        val isUnusable = unavailable
    }

    class SPI(
        val name: String
    ) : SirAttribute {
        override val identifier: String get() = "_spi"
        override val arguments: List<SirArgument> get() = listOf(SirArgument(name))

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SPI) return false

            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }

    object NonOverride : SirAttribute {
        override val identifier: String get() = "_nonoverride"

        override val arguments: List<SirArgument>? get() = null
    }

    object Escaping : SirAttribute, SirFunctionalTypeAttribute {
        override val identifier: String get() = "escaping"
        override val arguments: List<SirArgument>? get() = null

        override fun isPrintableInPosition(position: SirTypeVariance): Boolean = position == SirTypeVariance.CONTRAVARIANT
    }

    class ObjC(val name: String?) : SirAttribute {
        override val identifier: String get() = "objc"
        override val arguments: List<SirArgument> get() = listOfNotNull(name?.let { SirArgument(name) })
    }
}
