/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.scala.silicon.ast.BuiltinField
import org.jetbrains.kotlin.formver.scala.silicon.ast.Type

object SpecialFields {
    val FunctionObjectCallCounterField = BuiltinField(SpecialFieldName("function_object_call_counter"), Type.Int)
    val all = listOf(
        FunctionObjectCallCounterField
    )
}