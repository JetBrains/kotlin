/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.domains

import org.jetbrains.kotlin.formver.names.SpecialName
import org.jetbrains.kotlin.formver.viper.ast.*

/**
 * DSL language for construction of Viper functions.
 * Should be used via `build` method of companion object.
 * It is mandatory that `build` clause will have exactly one `returns` subclause.
 *
 * After `argument` clause arguments can be used via `arg1`, `arg2`.
 * After `returns` clause result can be used via `result`.
 */
internal class FunctionBuilder private constructor() {
    private val pres = mutableListOf<Exp>()
    private val posts = mutableListOf<Exp>()
    private val formalArgs = mutableListOf<Var>()
    private lateinit var retType: Type
    private var functionBody: Exp? = null

    val arg1
        get() = formalArgs[0].use()

    val arg2
        get() = formalArgs[1].use()

    val result
        get() = Exp.Result(retType)

    companion object {
        fun build(name: String, action: FunctionBuilder.() -> Unit): BuiltinFunction {
            val builder = FunctionBuilder()
            builder.action()
            return object : BuiltinFunction(SpecialName(name)) {
                override val formalArgs = builder.formalArgs.map { it.decl() }
                override val retType: Type = builder.retType
                override val body = builder.functionBody
                override val pres = builder.pres
                override val posts = builder.posts
            }
        }
    }

    fun precondition(action: () -> Exp): Exp {
        val exp = action()
        pres.add(exp)
        return exp
    }

    fun precondition(exp: Exp) = precondition { exp }

    fun postcondition(action: () -> Exp): Exp {
        val exp = action()
        posts.add(exp)
        return exp
    }

    fun postcondition(exp: Exp) = postcondition { exp }

    fun argument(action: () -> Type): Exp.LocalVar {
        val argType = action()
        val variable = Var("arg${formalArgs.size + 1}", argType)
        formalArgs.add(variable)
        return variable.use()
    }

    fun argument(type: Type) = argument { type }

    fun returns(action: () -> Type): Exp.Result {
        val type = action()
        retType = type
        return Exp.Result(type)
    }

    fun returns(type: Type) = returns { type }

    fun body(action: () -> Exp): Exp {
        val exp = action()
        functionBody = exp
        return exp
    }

    fun body(exp: Exp) = body { exp }
}
