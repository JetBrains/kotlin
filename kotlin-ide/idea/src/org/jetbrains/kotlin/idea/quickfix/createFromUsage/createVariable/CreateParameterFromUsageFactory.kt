/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactoryWithDelegate
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo
import org.jetbrains.kotlin.psi.KtElement

data class CreateParameterData<out E : KtElement>(
    val parameterInfo: KotlinParameterInfo,
    val originalExpression: E,
    val createSilently: Boolean = false,
    val onComplete: ((Editor?) -> Unit)? = null
)

abstract class CreateParameterFromUsageFactory<E : KtElement> :
    KotlinSingleIntentionActionFactoryWithDelegate<E, CreateParameterData<E>>() {
    override fun createFix(originalElement: E, data: CreateParameterData<E>) = CreateParameterFromUsageFix(data)
}