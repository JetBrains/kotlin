/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.ExpWrapper
import org.jetbrains.kotlin.formver.embeddings.expression.GeCmp
import org.jetbrains.kotlin.formver.embeddings.expression.IntLit
import org.jetbrains.kotlin.formver.names.NameMatcher
import org.jetbrains.kotlin.formver.names.ScopedKotlinName
import org.jetbrains.kotlin.formver.names.SpecialName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Exp
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

    fun extraAccessInvariantsForParameter(v: Exp): List<ExpEmbedding> = listOf()

    fun accessInvariantsForParameter(v: Exp): List<ExpEmbedding> =
        when (accessPolicy) {
            AccessPolicy.ALWAYS_INHALE_EXHALE -> listOf()
            AccessPolicy.ALWAYS_READABLE -> listOf(
                ExpWrapper(
                    v.fieldAccessPredicate(toViper(), PermExp.WildcardPerm()),
                    BooleanTypeEmbedding
                )
            )
            AccessPolicy.ALWAYS_WRITEABLE -> listOf(ExpWrapper(v.fieldAccessPredicate(toViper(), PermExp.FullPerm()), BooleanTypeEmbedding))
        } + extraAccessInvariantsForParameter(v)

    fun accessInvariantForAccess(v: Exp): ExpEmbedding? =
        when (accessPolicy) {
            AccessPolicy.ALWAYS_INHALE_EXHALE -> ExpWrapper(v.fieldAccessPredicate(toViper(), PermExp.FullPerm()), BooleanTypeEmbedding)
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
    override fun extraAccessInvariantsForParameter(v: Exp): List<ExpEmbedding> =
        listOf(GeCmp(ExpWrapper(v.fieldAccess(toViper()), IntTypeEmbedding), IntLit(0)))
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