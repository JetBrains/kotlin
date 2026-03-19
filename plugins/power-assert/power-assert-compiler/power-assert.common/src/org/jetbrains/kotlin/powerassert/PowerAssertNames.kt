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

    @JvmField
    val POWER_ASSERT_FQ_NAME = POWER_ASSERT_PACKAGE_FQ_NAME.child(Name.identifier("PowerAssert"))

    @JvmField
    val POWER_ASSERT_CLASS_ID = ClassId.topLevel(POWER_ASSERT_FQ_NAME)

    private val POWER_ASSERT_COMPANION_CLASS_ID = POWER_ASSERT_CLASS_ID.createNestedClassId(Name.identifier("Companion"))

    @JvmField
    val POWER_ASSERT_EXPLANATION_CALLABLE_ID = CallableId(POWER_ASSERT_COMPANION_CLASS_ID, Name.identifier("explanation"))

    @JvmField
    val POWER_ASSERT_EXPLANATION_GETTER_CALLABLE_ID = CallableId(POWER_ASSERT_COMPANION_CLASS_ID, Name.special("<get-explanation>"))

    @JvmField
    val POWER_ASSERT_IGNORE_CLASS_ID = POWER_ASSERT_CLASS_ID.createNestedClassId(Name.identifier("Ignore"))

    @JvmField
    val CALL_EXPLANATION_CLASS_ID = ClassId.topLevel(POWER_ASSERT_PACKAGE_FQ_NAME.child(Name.identifier("CallExplanation")))
}
