/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.formver.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.FieldAccess
import org.jetbrains.kotlin.formver.embeddings.expression.GeCmp
import org.jetbrains.kotlin.formver.embeddings.expression.IntLit
import org.jetbrains.kotlin.formver.names.NameMatcher
import org.jetbrains.kotlin.formver.names.ScopedKotlinName
import org.jetbrains.kotlin.formver.names.SpecialName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Field
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Type
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

/**
 * Embedding of a backing field of a property.
 */
interface FieldEmbedding {
    val name: MangledName
    val type: TypeEmbedding
    val viperType: Type
    val accessPolicy: AccessPolicy
    val isUnique: Boolean
        get() = false

    // If true, it is necessary to unfold the predicate of the receiver before accessing the field
    val unfoldToAccess: Boolean
        get() = false
    val containingClass: ClassTypeEmbedding?
        get() = null
    val includeInShortDump: Boolean
    val symbol: FirPropertySymbol?
        get() = null

    fun toViper(): Field = Field(name, viperType, includeInShortDump)

    fun extraAccessInvariantsForParameter(): List<TypeInvariantEmbedding> = listOf()

    fun accessInvariantsForParameter(): List<TypeInvariantEmbedding> =
        when (accessPolicy) {
            AccessPolicy.ALWAYS_WRITEABLE -> listOf(FieldAccessTypeInvariantEmbedding(this, PermExp.FullPerm()))
            AccessPolicy.ALWAYS_INHALE_EXHALE, AccessPolicy.ALWAYS_READABLE -> listOf()
        } + extraAccessInvariantsForParameter()

    fun accessInvariantForAccess(): TypeInvariantEmbedding? =
        when (accessPolicy) {
            AccessPolicy.ALWAYS_INHALE_EXHALE -> FieldAccessTypeInvariantEmbedding(this, PermExp.FullPerm())
            AccessPolicy.ALWAYS_READABLE, AccessPolicy.ALWAYS_WRITEABLE -> null
        }
}

class UserFieldEmbedding(
    override val name: ScopedKotlinName,
    override val type: TypeEmbedding,
    override val symbol: FirPropertySymbol,
    override val isUnique: Boolean,
    override val containingClass: ClassTypeEmbedding,
) : FieldEmbedding {
    override val viperType = Type.Ref
    override val accessPolicy: AccessPolicy = if (symbol.isVal) AccessPolicy.ALWAYS_READABLE else AccessPolicy.ALWAYS_INHALE_EXHALE
    override val unfoldToAccess: Boolean
        get() = accessPolicy == AccessPolicy.ALWAYS_READABLE
    override val includeInShortDump: Boolean = true
}


object ListSizeFieldEmbedding : FieldEmbedding {
    override val name = SpecialName("size")
    override val type = buildType { int() }
    override val viperType = Type.Ref
    override val accessPolicy = AccessPolicy.ALWAYS_WRITEABLE
    override val includeInShortDump: Boolean = true
    override fun extraAccessInvariantsForParameter(): List<TypeInvariantEmbedding> = listOf(NonNegativeSizeTypeInvariantEmbedding)

    object NonNegativeSizeTypeInvariantEmbedding : TypeInvariantEmbedding {
        override fun fillHole(exp: ExpEmbedding): ExpEmbedding =
            GeCmp(FieldAccess(exp, ListSizeFieldEmbedding), IntLit(0))
    }
}

fun ScopedKotlinName.specialEmbedding(embedding: ClassTypeEmbedding): FieldEmbedding? =
    NameMatcher.matchClassScope(this) {
        ifBackingFieldName("size") {
            return embedding.isCollectionInheritor.ifTrue {
                ListSizeFieldEmbedding
            }
        }
        return null
    }