/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.errors

import org.jetbrains.kotlin.formver.viper.ast.AstWrapper
import org.jetbrains.kotlin.formver.viper.ast.Position

class VerificationError private constructor(
    val result: viper.silver.verifier.VerificationError,
) : VerifierError {
    companion object {
        fun fromSilver(result: viper.silicon.interfaces.VerificationResult): VerificationError {
            check(result.isFatal) { "The verification result must contain an error to be converted." }
            return VerificationError((result as viper.silicon.interfaces.Failure).message())
        }
    }

    /**
     * The [locationNode] represents the node in Viper's AST where the error occurred.
     * This is useful with error reporting, when we want to fetch missing information from the error,
     * and we need to reconstruct the original Kotlin's code context.
     */
    val locationNode: AstWrapper.Node
        get() = AstWrapper.Node(result.offendingNode())

    /**
     * The [unverifiableProposition] represents the proposition that could not be verified in the Viper's AST.
     */
    val unverifiableProposition: AstWrapper.Exp
        get() = AstWrapper.Exp(result.reason().offendingNode())
    override val id: String
        get() = result.id()
    override val msg: String
        get() = result.readableMessage(false, false)
    override val position: Position
        get() = Position.fromSilver(result.pos())
}
