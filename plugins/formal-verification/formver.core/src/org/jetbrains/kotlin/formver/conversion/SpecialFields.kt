/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.viper.ast.BuiltinField
import org.jetbrains.kotlin.formver.viper.ast.Type

object SpecialFields {
    val FunctionObjectCallCounterField = BuiltinField(SpecialName("function_object_call_counter"), Type.Int)
    val all = listOf(
        FunctionObjectCallCounterField,
    )
}