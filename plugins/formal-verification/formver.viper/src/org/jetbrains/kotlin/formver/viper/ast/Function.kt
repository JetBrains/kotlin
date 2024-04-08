/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*


/**
 * We want to deal with Viper's binary operators, functions and domain functions in a similar manner, hence introducing this common interface.
 */
interface Applicable {
    fun toFuncApp(args: List<Exp>, pos: Position = Position.NoPosition, info: Info = Info.NoInfo, trafos: Trafos = Trafos.NoTrafos): Exp

    operator fun invoke(vararg args: Exp, pos: Position = Position.NoPosition, info: Info = Info.NoInfo, trafos: Trafos = Trafos.NoTrafos) =
        toFuncApp(args.toList(), pos, info, trafos)
}

interface Function : IntoSilver<viper.silver.ast.Function>, Applicable {
    val name: MangledName
    val pos: Position
        get() = Position.NoPosition
    val info: Info
        get() = Info.NoInfo
    val trafos: Trafos
        get() = Trafos.NoTrafos
    val includeInDumpPolicy: IncludeInDumpPolicy
    val formalArgs: List<Declaration.LocalVarDecl>
    val retType: Type
    val pres: List<Exp>
        get() = listOf()
    val posts: List<Exp>
        get() = listOf()
    val body: Exp?
        get() = null

    override fun toSilver(): viper.silver.ast.Function = viper.silver.ast.Function(
        name.mangled, formalArgs.map { it.toSilver() }.toScalaSeq(),
        retType.toSilver(), pres.toSilver().toScalaSeq(), posts.toSilver().toScalaSeq(), body.toScalaOption().toSilver(),
        pos.toSilver(), info.toSilver(), trafos.toSilver()
    )

    override fun toFuncApp(
        args: List<Exp>,
        pos: Position,
        info: Info,
        trafos: Trafos,
    ): Exp.FuncApp = Exp.FuncApp(name, args, retType, pos, info, trafos)
}

abstract class BuiltinFunction(
    override val name: MangledName,
    override val pos: Position = Position.NoPosition,
    override val info: Info = Info.NoInfo,
    override val trafos: Trafos = Trafos.NoTrafos,
) : Function {
    override val includeInDumpPolicy: IncludeInDumpPolicy = IncludeInDumpPolicy.ONLY_IN_FULL_DUMP
}


/**
 * These are function-like classes which are not translated to Viper as function calls but as arithmetic and/or boolean operations.
 */
interface Operator : Applicable
