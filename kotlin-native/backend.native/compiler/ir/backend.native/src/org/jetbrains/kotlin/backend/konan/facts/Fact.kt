/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.facts

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.render

@JvmInline
value class SymbolKey(val signatureHash: ULong) {
    companion object {
        fun of(signature: IdSignature): SymbolKey {
            val signatureBuffer = signature.render().toByteArray()
            return SymbolKey(cityHash64(signatureBuffer))
        }
    }
}

enum class FactKind(val id: Int) {
    SIGNATURE(0),
    BASE_CLASS_LAYOUT(1),
    INLINE_BODY(5)
}

class FactKey(
        val kind: FactKind,
        val key: SymbolKey,
        val lazyRepresentation: (() -> String?)? = null
) {
    val representation: String?
        get() = lazyRepresentation?.invoke()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FactKey

        if (kind != other.kind) return false
        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        var result = kind.hashCode()
        result = 31 * result + key.hashCode()
        return result
    }
}

sealed class Fact {
    abstract val irSymbol: IrSymbol
    abstract val irFilePathString: String
    abstract val kind: FactKind

    internal val factKey: FactKey
        get() {
            val signature = irSymbol.signature ?: irSymbol.privateSignature ?: error("The given symbol has no signature")
            val symbolKey = SymbolKey.of(signature)
            return FactKey(kind, symbolKey) { signature.render() }
        }

    // This function allows us to determine ABI changes easier.
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
                // Not exclusive: e.g. a function is overridable and has a visibility too.
                if (d is IrFunction) append('|').append(d.returnType.renderForFacts())
                // A call site lowers the constructor calls of a value class (inlined) differently from the other ones.
                if (d is IrConstructor) append('|').append(d.constructedClass.isValue)
                // A call site calls a final function directly, and an open one through its vtable trampoline.
                if (d is IrOverridableDeclaration<*>) append('|').append(d.modality)
                if (d is IrDeclarationWithVisibility) append('|').append(d.visibility)
            }
            return cityHash64(text.toByteArray())
        }
    }

    class InlineBody(
            override val irSymbol: IrSymbol,
            override val irFilePathString: String
    ) : Fact() {

        companion object {
            fun from(irFunction: IrFunction): InlineBody =
                    InlineBody(
                            irSymbol = irFunction.symbol,
                            irFilePathString = irFunction.file.path
                    )
        }

        override val kind: FactKind = FactKind.INLINE_BODY

        override fun contentHash(): ULong {
            // For inline functions we should not care about the signature (this is already covered by Fact.Signature).
            return cityHash64((irSymbol.owner as IrFunction).body!!.dump().toByteArray())
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
                // Fields: still inside their properties before lowering, moved to the class after it.
                val fields = c.declarations.flatMap {
                    when (it) {
                        is IrField -> listOf(it)
                        is IrProperty -> listOfNotNull(it.backingField)
                        else -> emptyList()
                    }
                }.distinct()
                append('(').append(
                        fields.joinToString(",") { "${it.name}:${it.type.renderForFacts()}" }
                ).append(')')
                // Parent types
                append("<:").append(
                        c.superTypes.joinToString(",") { it.renderForFacts() }
                )
            }
            return cityHash64(text.toByteArray())
        }
    }
}

/**
 * Renders a type by the signatures of its classifiers, unlike [render], which needs them to be bound:
 * the classifiers from other klibs are bound when compiling, but not in [KlibFactsGenerator].
 * Type annotations are ignored.
 */
private fun IrType.renderForFacts(): String {
    if (this !is IrSimpleType) return render()
    return buildString {
        val classifierSignature = classifier.signature ?: classifier.privateSignature
        when {
            classifierSignature != null -> append(classifierSignature.render())
            else -> append((classifier.owner as? IrTypeParameter)?.let { "${it.name}#${it.index}" } ?: render())
        }
        if (arguments.isNotEmpty()) {
            append(arguments.joinToString(",", "<", ">") {
                when (it) {
                    is IrStarProjection -> "*"
                    is IrTypeProjection -> "${it.variance} ${it.type.renderForFacts()}"
                }
            })
        }
        append(' ').append(nullability)
    }
}
