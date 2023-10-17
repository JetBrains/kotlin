/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.errors

import org.jetbrains.kotlin.formver.viper.ast.Position

/**
 * Interface adapter between Kotlin and Silver.
 */
interface VerifierError {
    val id: String
    val msg: String
    val position: Position
}
