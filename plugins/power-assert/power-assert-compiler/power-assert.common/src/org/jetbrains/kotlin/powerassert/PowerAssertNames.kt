/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object PowerAssertNames {
    @JvmField
    val POWER_ASSERT_PACKAGE_FQ_NAME = FqName("kotlin.powerassert")

    // ===== PowerAssert annotation and nested symbols ===== //

    @JvmField
    val POWER_ASSERT_FQ_NAME = POWER_ASSERT_PACKAGE_FQ_NAME.child(Name.identifier("PowerAssert"))

    @JvmField
    val POWER_ASSERT_CLASS_ID = ClassId.topLevel(POWER_ASSERT_FQ_NAME)

    private val POWER_ASSERT_COMPANION_CLASS_ID = POWER_ASSERT_CLASS_ID.createNestedClassId(Name.identifier("Companion"))

    @JvmField
    val POWER_ASSERT_EXPLANATION_CALLABLE_ID = CallableId(POWER_ASSERT_COMPANION_CLASS_ID, Name.identifier("explanation"))

    @JvmField
    val POWER_ASSERT_IGNORE_CLASS_ID = POWER_ASSERT_CLASS_ID.createNestedClassId(Name.identifier("Ignore"))

    // ===== CallExplanation class and nested symbols ===== //

    @JvmField
    val CALL_EXPLANATION_CLASS_ID = ClassId.topLevel(POWER_ASSERT_PACKAGE_FQ_NAME.child(Name.identifier("CallExplanation")))

    @JvmField
    val CALL_EXPLANATION_ARGUMENT_CLASS_ID = CALL_EXPLANATION_CLASS_ID.createNestedClassId(Name.identifier("Argument"))

    @JvmField
    val CALL_EXPLANATION_ARGUMENT_KIND_CLASS_ID = CALL_EXPLANATION_ARGUMENT_CLASS_ID.createNestedClassId(Name.identifier("Kind"))

    // ===== Expression classes ===== //

    @JvmField
    val EXPRESSION_CLASS_ID = ClassId.topLevel(POWER_ASSERT_PACKAGE_FQ_NAME.child(Name.identifier("Expression")))

    @JvmField
    val VALUE_EXPRESSION_CLASS_ID = ClassId.topLevel(POWER_ASSERT_PACKAGE_FQ_NAME.child(Name.identifier("ValueExpression")))

    @JvmField
    val LITERAL_EXPRESSION_CLASS_ID = ClassId.topLevel(POWER_ASSERT_PACKAGE_FQ_NAME.child(Name.identifier("LiteralExpression")))

    @JvmField
    val EQUALITY_EXPRESSION_CLASS_ID = ClassId.topLevel(POWER_ASSERT_PACKAGE_FQ_NAME.child(Name.identifier("EqualityExpression")))

    // ===== Utility functions ===== //

    @JvmField
    val EXPLANATION_TO_DEFAULT_MESSAGE_CALLABLE_ID = CallableId(POWER_ASSERT_PACKAGE_FQ_NAME, Name.identifier("toDefaultMessage"))
}
