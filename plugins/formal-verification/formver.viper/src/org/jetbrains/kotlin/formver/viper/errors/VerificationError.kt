/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.errors

import org.jetbrains.kotlin.formver.viper.ast.Position
import viper.silver.verifier.errors


sealed interface VerificationError : VerifierError {
    val result: viper.silver.verifier.AbstractVerificationError
    override val id: String
        get() = result.id()
    override val msg: String
        get() = result.readableMessage(false, false)
    override val position: Position
        get() = Position.fromSilver(result.pos())
}

data class PreconditionInCallFalse(
    override val result: errors.PreconditionInCallFalse,
) : VerificationError

data class PostconditionViolated(
    override val result: errors.PostconditionViolated,
) : VerificationError

data class AssertFailed(
    override val result: errors.AssertFailed,
) : VerificationError

object ErrorAdapter {
    fun translate(result: viper.silicon.interfaces.VerificationResult): VerificationError {
        assert(result.isFatal)
        return when (val err = (result as viper.silicon.interfaces.Failure).message()) {
            is errors.PreconditionInCallFalse -> PreconditionInCallFalse(err)
            is errors.PostconditionViolated -> PostconditionViolated(err)
            is errors.AssertFailed -> AssertFailed(err)
            else -> TODO("Unhandled verification error.")
        }
    }
}