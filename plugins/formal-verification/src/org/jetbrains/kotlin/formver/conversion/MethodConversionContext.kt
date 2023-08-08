/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.formver.scala.silicon.ast.Stmt
import viper.silver.ast.Method

/**
 * Contains the metadata for converting a single (specific) Kotlin method;
 * create a new instance for each method if you want to convert multiple.
 * Note that by default we do not convert the whole method: we expect to
 * only need the signature in most methods, as we verify methods one at a
 * time.
 */
class MethodConversionContext(val programCtx: ProgramConversionContext, val signature: ConvertedMethodSignature, val body: FirBlock?) {
    // Converting the body here would be too late; we want this to be a pure method, while
    // converting the body may involve the program context.
    val toMethod: Method
        get() =
            signature.toMethod(listOf(), listOf(), convertedBody)

    val returnVar: ConvertedVar?
        get() = signature.returnVar

    private val convertedBody = body?.let { convertBody(it) }

    private fun convertBody(body: FirBlock): Stmt.Seqn {
        val ctx = StmtConversionContext(this)
        ctx.convertAndAppend(body)
        return ctx.block
    }

    private var nextAnonVarNumber = 0

    fun newAnonVar(type: ConvertedType): ConvertedVar =
        ConvertedVar(AnonymousName(++nextAnonVarNumber), type)
}