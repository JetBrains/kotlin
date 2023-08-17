/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.scala.IntoViper
import org.jetbrains.kotlin.formver.scala.toScalaMap
import org.jetbrains.kotlin.formver.scala.toScalaSeq
import viper.silver.ast.*

sealed interface Type : IntoViper<viper.silver.ast.Type> {
    data object Int : Type {
        override fun toViper(): viper.silver.ast.Type = `Int$`.`MODULE$`
    }

    data object Bool : Type {
        override fun toViper(): viper.silver.ast.Type = `Bool$`.`MODULE$`
    }

    data object Perm : Type {
        override fun toViper(): viper.silver.ast.Type = `Perm$`.`MODULE$`
    }

    data object Ref : Type {
        override fun toViper(): viper.silver.ast.Type = `Ref$`.`MODULE$`
    }

    data object Wand : Type {
        override fun toViper(): viper.silver.ast.Type = `Wand$`.`MODULE$`
    }

    data class Seq(val type: Type) : Type {
        override fun toViper(): viper.silver.ast.Type = SeqType.apply(type.toViper())
    }

    data class Set(val type: Type) : Type {
        override fun toViper(): viper.silver.ast.Type = SetType.apply(type.toViper())
    }

    data class Multiset(val type: Type) : Type {
        override fun toViper(): viper.silver.ast.Type = MultisetType.apply(type.toViper())
    }

    data class Map(val keyType: Type, val valueType: Type) : Type {
        override fun toViper(): viper.silver.ast.Type = MapType.apply(keyType.toViper(), valueType.toViper())
    }

    data class TypeVar(val name: String) : Type {
        override fun toViper(): viper.silver.ast.TypeVar =
            viper.silver.ast.TypeVar(name)
    }

    data class Domain(
        val domainName: String,
        val typeParams: List<TypeVar> = emptyList(),
        val typeSubstitutions: kotlin.collections.Map<TypeVar, Type> = emptyMap(),
    ) : Type {
        override fun toViper(): DomainType =
            DomainType(
                domainName,
                typeSubstitutions.mapKeys { it.key.toViper() }.mapValues { it.value.toViper() }.toScalaMap(),
                typeParams.map { it.toViper() }.toScalaSeq()
            )
    }

}
