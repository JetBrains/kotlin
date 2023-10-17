/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.errors

import org.jetbrains.kotlin.formver.viper.ast.Position

sealed interface ConsistencyError : VerifierError {
    val error: viper.silver.verifier.ConsistencyError
    override val id: String
        get() = error.fullId()
    override val msg: String
        get() = error.readableMessage()
    override val position: Position
        get() = Position.fromSilver(error.pos())
}

data class GenericConsistencyError(
    override val error: viper.silver.verifier.ConsistencyError
) : ConsistencyError