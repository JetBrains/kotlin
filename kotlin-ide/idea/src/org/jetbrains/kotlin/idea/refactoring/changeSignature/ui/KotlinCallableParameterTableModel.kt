/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.idea.refactoring.changeSignature.ui

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiCodeFragment
import com.intellij.psi.PsiElement
import com.intellij.refactoring.changeSignature.ParameterTableModelBase
import com.intellij.refactoring.changeSignature.ParameterTableModelItemBase
import com.intellij.util.ui.ColumnInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.psi.KtPsiFactory

abstract class KotlinCallableParameterTableModel protected constructor(
    private val methodDescriptor: KotlinMethodDescriptor,
    typeContext: PsiElement,
    defaultValueContext: PsiElement,
    vararg columnInfos: ColumnInfo<*, *>?
) : ParameterTableModelBase<KotlinParameterInfo, ParameterTableModelItemBase<KotlinParameterInfo>>(
    typeContext,
    defaultValueContext,
    *columnInfos,
) {
    private val project: Project = typeContext.project

    open var receiver: KotlinParameterInfo? = null

    override fun createRowItem(parameterInfo: KotlinParameterInfo?): ParameterTableModelItemBase<KotlinParameterInfo> {
        val resultParameterInfo = parameterInfo ?: KotlinParameterInfo(
            methodDescriptor.baseDescriptor,
            -1,
            "",
            KotlinTypeInfo(false, null, null),
            null,
            null,
            KotlinValVar.None,
            null
        )

        val psiFactory = KtPsiFactory(project)
        val paramTypeCodeFragment: PsiCodeFragment = psiFactory.createTypeCodeFragment(
            resultParameterInfo.currentTypeInfo.render(),
            myTypeContext,
        )

        val defaultValueForCall = resultParameterInfo.defaultValueForCall
        val defaultValueCodeFragment: PsiCodeFragment = psiFactory.createExpressionCodeFragment(
            if (defaultValueForCall != null) defaultValueForCall.text else "",
            myDefaultValueContext,
        )

        return object : ParameterTableModelItemBase<KotlinParameterInfo>(resultParameterInfo, paramTypeCodeFragment, defaultValueCodeFragment) {
            override fun isEllipsisType(): Boolean = false
        }
    }

    companion object {
        fun isTypeColumn(column: ColumnInfo<*, *>?): Boolean = column is TypeColumn<*, *>

        fun isNameColumn(column: ColumnInfo<*, *>?): Boolean = column is NameColumn<*, *>

        fun isDefaultValueColumn(column: ColumnInfo<*, *>?): Boolean = column is DefaultValueColumn<*, *>
    }

}