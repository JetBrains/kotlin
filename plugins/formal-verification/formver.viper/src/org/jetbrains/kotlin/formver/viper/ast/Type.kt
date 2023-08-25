/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoViper
import org.jetbrains.kotlin.formver.viper.domains.NullableDomain
import org.jetbrains.kotlin.formver.viper.domains.UnitDomain
import org.jetbrains.kotlin.formver.viper.toScalaMap
import org.jetbrains.kotlin.formver.viper.toScalaSeq
import viper.silver.ast.*

sealed interface Type : IntoViper<viper.silver.ast.Type> {

    fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Type

    data object Int : Type {
        override fun toViper(): viper.silver.ast.Type = `Int$`.`MODULE$`
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Int = Int
    }

    data object Bool : Type {
        override fun toViper(): viper.silver.ast.Type = `Bool$`.`MODULE$`
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Bool = Bool
    }

    data object Perm : Type {
        override fun toViper(): viper.silver.ast.Type = `Perm$`.`MODULE$`
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Perm = Perm
    }

    data object Ref : Type {
        override fun toViper(): viper.silver.ast.Type = `Ref$`.`MODULE$`
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Ref = Ref
    }

    data object Wand : Type {
        override fun toViper(): viper.silver.ast.Type = `Wand$`.`MODULE$`
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Wand = Wand
    }

    data class Seq(val elemType: Type) : Type {
        override fun toViper(): viper.silver.ast.Type = SeqType.apply(elemType.toViper())
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Seq = Seq(elemType.substitute(typeVarMap))
    }

    data class Set(val elemType: Type) : Type {
        override fun toViper(): viper.silver.ast.Type = SetType.apply(elemType.toViper())
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Set = Set(elemType.substitute(typeVarMap))
    }

    data class Multiset(val elemType: Type) : Type {
        override fun toViper(): viper.silver.ast.Type = MultisetType.apply(elemType.toViper())
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Multiset = Multiset(elemType.substitute(typeVarMap))
    }

    data class Map(val keyType: Type, val valueType: Type) : Type {
        override fun toViper(): viper.silver.ast.Type = MapType.apply(keyType.toViper(), valueType.toViper())
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Map =
            Map(keyType.substitute(typeVarMap), valueType.substitute(typeVarMap))

    }

    data class TypeVar(val name: String) : Type {
        override fun toViper(): viper.silver.ast.TypeVar =
            viper.silver.ast.TypeVar(name)

        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>) = typeVarMap.getOrDefault(this, this)
    }

    data class Domain(
        val domainName: String,
        val typeParams: List<TypeVar> = emptyList(),
        val typeSubstitutions: kotlin.collections.Map<TypeVar, Type> = emptyMap(),
    ) : Type {
        override fun toViper(): DomainType =
            DomainType.apply(
                domainName,
                typeSubstitutions.mapKeys { it.key.toViper() }.mapValues { it.value.toViper() }.toScalaMap(),
                typeParams.map { it.toViper() }.toScalaSeq()
            )

        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Domain =
            Domain(domainName, typeParams, typeParams.associateWith { typeSubstitutions.getOrDefault(it, it).substitute(typeVarMap) })

    }

}
