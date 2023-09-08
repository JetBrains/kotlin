/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.viper.ast.Label

interface WhileStackContext<out RTC : ResultTrackingContext> {
    val continueLabel: Label
    val breakLabel: Label
    fun inNewWhileBlock(action: (StmtConversionContext<RTC>) -> Unit)
}