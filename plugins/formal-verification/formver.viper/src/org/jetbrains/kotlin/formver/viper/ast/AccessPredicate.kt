/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

sealed interface AccessPredicate : Exp {
    override fun toSilver(): viper.silver.ast.AccessPredicate

    data class FieldAccessPredicate(
        val access: Exp.FieldAccess,
        val perm: PermExp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : AccessPredicate {
        override val type: Type.Bool = Type.Bool
        override fun toSilver(): viper.silver.ast.AccessPredicate =
            viper.silver.ast.FieldAccessPredicate(access.toSilver(), perm.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }
}