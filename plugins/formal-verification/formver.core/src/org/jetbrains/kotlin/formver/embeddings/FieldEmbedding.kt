/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

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

/**
 * Embedding of a backing field of a property.
 */
interface FieldEmbedding {
    val name: MangledName
    val type: TypeEmbedding
    val accessPolicy: AccessPolicy
    val includeInShortDump: Boolean

    fun toViper(): Field = Field(name, type.viperType, includeInShortDump)

    fun extraAccessInvariantsForParameter(): List<TypeInvariantEmbedding> = listOf()

    fun accessInvariantsForParameter(): List<TypeInvariantEmbedding> =
        when (accessPolicy) {
            AccessPolicy.ALWAYS_INHALE_EXHALE -> listOf()
            AccessPolicy.ALWAYS_READABLE -> listOf(
                FieldAccessTypeInvariantEmbedding(this, PermExp.WildcardPerm())
            )
            AccessPolicy.ALWAYS_WRITEABLE -> listOf(FieldAccessTypeInvariantEmbedding(this, PermExp.FullPerm()))
        } + extraAccessInvariantsForParameter()

    fun accessInvariantForAccess(): TypeInvariantEmbedding? =
        when (accessPolicy) {
            AccessPolicy.ALWAYS_INHALE_EXHALE -> FieldAccessTypeInvariantEmbedding(this, PermExp.FullPerm())
            AccessPolicy.ALWAYS_READABLE, AccessPolicy.ALWAYS_WRITEABLE -> null
        }
}

class UserFieldEmbedding(override val name: ScopedKotlinName, override val type: TypeEmbedding, readOnly: Boolean) : FieldEmbedding {
    override val accessPolicy: AccessPolicy = if (readOnly) AccessPolicy.ALWAYS_READABLE else AccessPolicy.ALWAYS_INHALE_EXHALE
    override val includeInShortDump: Boolean = true
}

object ListSizeFieldEmbedding : FieldEmbedding {
    override val name = SpecialName("size")
    override val type = IntTypeEmbedding
    override val accessPolicy = AccessPolicy.ALWAYS_WRITEABLE
    override val includeInShortDump: Boolean = true
    override fun extraAccessInvariantsForParameter(): List<TypeInvariantEmbedding> = listOf(NonNegativeSizeTypeInvariantEmbedding)

    object NonNegativeSizeTypeInvariantEmbedding : TypeInvariantEmbedding {
        override fun fillHole(exp: ExpEmbedding): ExpEmbedding =
            GeCmp(FieldAccess(exp, ListSizeFieldEmbedding), IntLit(0))
    }
}

fun ScopedKotlinName.specialEmbedding(): FieldEmbedding? =
    NameMatcher.match(this) {
        ifIsCollectionInterface {
            ifMemberName("size") {
                return ListSizeFieldEmbedding
            }
        }
        return null
    }