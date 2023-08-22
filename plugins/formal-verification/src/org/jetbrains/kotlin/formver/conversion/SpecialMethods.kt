/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.scala.silicon.ast.*
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp.*
import org.jetbrains.kotlin.formver.scala.toScalaBigInt

private fun invokeFunctionObject(): BuiltInMethod {
    val thisArg = LocalVar(AnonymousName(0), Type.Ref)
    val acc = AccessPredicate.FieldAccessPredicate(
        thisArg.fieldAccess(SpecialFields.FunctionObjectCallCounterField),
        PermExp.FullPerm()
    )
    val calls = EqCmp(
        Add(Old(thisArg.fieldAccess(SpecialFields.FunctionObjectCallCounterField)), IntLit(1.toScalaBigInt())),
        thisArg.fieldAccess(SpecialFields.FunctionObjectCallCounterField)
    )
    return BuiltInMethod(
        InvokeFunctionObjectName,
        listOf(Declaration.LocalVarDecl(AnonymousName(0), Type.Ref)),
        listOf(),
        listOf(acc),
        listOf(acc, calls),
        null
    )
}

object SpecialMethods {
    val all = listOf(invokeFunctionObject())
}