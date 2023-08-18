/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

sealed interface AccessPredicate : Exp {
    override fun toViper(): viper.silver.ast.AccessPredicate

    data class FieldAccessPredicate(
        val access: Exp.FieldAccess,
        val perm: PermExp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : AccessPredicate {
        override fun toViper(): viper.silver.ast.AccessPredicate =
            viper.silver.ast.FieldAccessPredicate(access.toViper(), perm.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }
}