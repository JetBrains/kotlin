/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.conversion.SpecialName
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

    fun toViper(): Field = Field(name, type.viperType)

    fun extraAccessInvariantsForParameter(v: Exp): List<Exp> = listOf()

    fun accessInvariantsForParameter(v: Exp): List<Exp> =
        when (accessPolicy) {
            AccessPolicy.ALWAYS_INHALE_EXHALE -> listOf()
            AccessPolicy.ALWAYS_READABLE -> listOf(v.fieldAccessPredicate(toViper(), PermExp.WildcardPerm()))
            AccessPolicy.ALWAYS_WRITEABLE -> listOf(v.fieldAccessPredicate(toViper(), PermExp.FullPerm()))
        } + extraAccessInvariantsForParameter(v)

    fun accessInvariantForAccess(v: Exp): Exp? =
        when (accessPolicy) {
            AccessPolicy.ALWAYS_INHALE_EXHALE -> v.fieldAccessPredicate(toViper(), PermExp.FullPerm())
            AccessPolicy.ALWAYS_READABLE, AccessPolicy.ALWAYS_WRITEABLE -> null
        }
}

class UserFieldEmbedding(override val name: ScopedKotlinName, override val type: TypeEmbedding, readOnly: Boolean) : FieldEmbedding {
    override val accessPolicy: AccessPolicy = if (readOnly) AccessPolicy.ALWAYS_READABLE else AccessPolicy.ALWAYS_INHALE_EXHALE
}

object ListSizeFieldEmbedding : FieldEmbedding {
    override val name = SpecialName("size")
    override val type = IntTypeEmbedding

    override val accessPolicy = AccessPolicy.ALWAYS_WRITEABLE
    override fun extraAccessInvariantsForParameter(v: Exp): List<Exp> =
        listOf(Exp.GeCmp(v.fieldAccess(toViper()), Exp.IntLit(0)))
}

fun ScopedKotlinName.specialEmbedding(): FieldEmbedding? {
    // in the future, new special properties can be added here (e.g. String.length)
    return when {
        isCollection -> when ((name as? MemberKotlinName)?.name.toString()) {
            "size" -> ListSizeFieldEmbedding
            else -> null
        }
        else -> null
    }
}