/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.facts

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.render

@JvmInline
value class SymbolKey(val signatureHash: ULong) {
    companion object {
        fun of(irSymbol: IrSymbol): SymbolKey? {
            val signatureBuffer = irSymbol.signature?.render()?.toByteArray() ?: return null
            return SymbolKey(cityHash64(signatureBuffer))
        }
    }
}

enum class FactKind(val id: Int) {
    SIGNATURE(0),
    BASE_CLASS_LAYOUT(1)
}

data class FactKey(val kind: FactKind, val key: SymbolKey) {
    /**
     * Debug-only human-readable representation (e.g., the rendered signature). Not part of the key's identity.
     */
    var representation: String? = null
        private set

    constructor(kind: FactKind, key: SymbolKey, representation: String?) : this(kind, key) {
        this.representation = representation
    }
}

sealed class Fact {
    abstract val irSymbol: IrSymbol
    abstract val irFilePathString: String
    abstract val kind: FactKind

    internal val factKey: FactKey?
        get() {
            val symbolKey = SymbolKey.of(irSymbol) ?: return null
            return FactKey(kind, symbolKey, irSymbol.signature?.render())
        }

    // Personal note: useful for ABI changes?
    abstract fun contentHash(): ULong

    class Signature(
            override val irSymbol: IrSymbol,
            override val irFilePathString: String
    ) : Fact() {
        companion object {
            fun from(irDeclaration: IrDeclaration): Signature =
                    Signature(
                            irSymbol = irDeclaration.symbol,
                            irFilePathString = irDeclaration.file.path
                    )
        }

        override val kind: FactKind = FactKind.SIGNATURE

        override fun contentHash(): ULong {
            val d = irSymbol.owner as IrDeclaration
            val text = buildString {
                append(irSymbol.signature?.render())
                when (d) {
                    is IrFunction -> append('|').append(d.returnType.render())
                    is IrOverridableDeclaration<*> -> append('|').append(d.modality)
                    is IrDeclarationWithVisibility -> append('|').append(d.visibility)
                }
            }
            return cityHash64(text.toByteArray())
        }
    }

    class BaseClassLayout(
            override val irSymbol: IrSymbol,
            override val irFilePathString: String
    ) : Fact() {
        companion object {
            fun from(irClass: IrClass): BaseClassLayout =
                    BaseClassLayout(
                            irSymbol = irClass.symbol,
                            irFilePathString = irClass.file.path
                    )
        }

        override val kind: FactKind = FactKind.BASE_CLASS_LAYOUT

        override fun contentHash(): ULong {
            val c = irSymbol.owner as IrClass
            val text = buildString {
                append(irSymbol.signature?.render())
                append('|').append(c.modality)
                append('|').append(c.visibility).append('|')
                // Fields
                append('(').append(
                        c.fields.joinToString(",") { "${it.name}:${it.type.render()}" }
                ).append(')')
                // Parent types
                append("<:").append(
                        c.superTypes.joinToString(",") { it.type.render() }
                )
            }
            return cityHash64(text.toByteArray())
        }
    }
}

