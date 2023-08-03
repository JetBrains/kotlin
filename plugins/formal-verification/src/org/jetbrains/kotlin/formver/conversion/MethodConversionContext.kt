/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.formver.scala.silicon.ast.Stmt
import viper.silver.ast.Method

/**
 * Contains the metadata for converting a single (specific) Kotlin method;
 * create a new instance for each method if you want to convert multiple.
 * Note that by default we do not convert the whole method: we expect to
 * only need the signature in most methods, as we verify methods one at a
 * time.
 */
class MethodConversionContext(val programCtx: ProgramConversionContext, val declaration: FirSimpleFunction) {
    val fullMethod: Method get() = signature.toMethod(listOf(), listOf(), convertedBody)
    val headerOnlyMethod: Method get() = signature.toMethod(listOf(), listOf(), null)

    val returnVar: ConvertedVar?
    val signature: ConvertedMethodSignature

    init {
        val retType = (declaration.returnTypeRef as FirResolvedTypeRef).type
        val convertedRetType = programCtx.convertType(retType)
        returnVar = (if (convertedRetType is ConvertedType) ConvertedVar(ReturnVariableName, convertedRetType) else null)

        val params = declaration.valueParameters.map {
            ConvertedVar(
                it.convertName(),
                programCtx.convertType((it.returnTypeRef as FirResolvedTypeRef).type) as ConvertedType
            )
        }
        val returns = returnVar?.let { listOf(it) } ?: emptyList()
        signature = ConvertedMethodSignature(declaration.symbol.callableId.convertName(), params, returns)
    }

    private val convertedBody: Stmt.Seqn
        get() {
            val body = declaration.body ?: throw Exception("Functions without a body are not supported yet.")
            val ctx = StmtConversionContext(this)
            ctx.convertAndAppend(body)
            return ctx.block
        }
}