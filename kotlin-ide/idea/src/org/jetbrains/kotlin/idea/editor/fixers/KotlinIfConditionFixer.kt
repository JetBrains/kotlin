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

package org.jetbrains.kotlin.idea.editor.fixers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtIfExpression

class KotlinIfConditionFixer : MissingConditionFixer<KtIfExpression>() {
    override val keyword = "if"
    override fun getElement(element: PsiElement?) = element as? KtIfExpression
    override fun getCondition(element: KtIfExpression) = element.condition
    override fun getLeftParenthesis(element: KtIfExpression) = element.leftParenthesis
    override fun getRightParenthesis(element: KtIfExpression) = element.rightParenthesis
    override fun getBody(element: KtIfExpression) = element.then
}
