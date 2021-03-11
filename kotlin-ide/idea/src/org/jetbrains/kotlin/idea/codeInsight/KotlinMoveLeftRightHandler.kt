/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.editorActions.moveLeftRight.MoveElementLeftRightHandler
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*

class KotlinMoveLeftRightHandler : MoveElementLeftRightHandler() {
    override fun getMovableSubElements(element: PsiElement): Array<PsiElement> {
        when (element) {
            is KtParameterList -> return element.parameters.toTypedArray()
            is KtValueArgumentList -> return element.arguments.toTypedArray()
            is KtArrayAccessExpression -> return element.indexExpressions.toTypedArray()
            is KtTypeParameterList -> return element.parameters.toTypedArray()
            is KtSuperTypeList -> return element.entries.toTypedArray()
            is KtTypeConstraintList -> return element.constraints.toTypedArray()
            //TODO
//            is KtClass -> if (element.isEnum()) return element.declarations.filterIsInstance<KtEnumEntry>().toTypedArray()
        }

        return emptyArray()
    }
}
