/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.errors

import org.jetbrains.kotlin.formver.viper.ast.Info
import org.jetbrains.kotlin.formver.viper.ast.Position
import org.jetbrains.kotlin.formver.viper.ast.info
import org.jetbrains.kotlin.formver.viper.ast.unwrapOr
import viper.silver.verifier.errors


sealed interface VerificationError : VerifierError {
    val result: viper.silver.verifier.VerificationError
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

data class UnknownError(
    override val result: viper.silver.verifier.VerificationError
) : VerificationError

object ErrorAdapter {
    fun translate(result: viper.silicon.interfaces.VerificationResult): VerificationError {
        assert(result.isFatal)
        return when (val err = (result as viper.silicon.interfaces.Failure).message()) {
            is errors.PreconditionInCallFalse -> PreconditionInCallFalse(err)
            is errors.PostconditionViolated -> PostconditionViolated(err)
            is errors.AssertFailed -> AssertFailed(err)
            else -> UnknownError(err)
        }
    }
}

/**
 * Given a verification error, find embedded extra information of type `I` in the
 * error's offending nodes.
 * If the extra info is not found, return `null`.
 * The information can be embedded either in the result's offending node,
 * or in the reason's offending node.
 * As an example, `PreconditionInCallFalse` errors have
 * as offending node result the call-site of the called method.
 * But the actual info we are interested in is on the pre-condition, contained in the reason's offending node.
 */
inline fun <reified I> VerificationError.getInfoOrNull(): I? =
    Info.fromSilver(result.offendingNode().info).unwrapOr<I> {
        Info.fromSilver(result.reason().offendingNode().info).unwrapOr<I> { null }
    }

