/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.DummyStringExpression
import org.jetbrains.jet.j2k.ast.Identifier
import com.intellij.psi.CommonClassNames.JAVA_LANG_OBJECT
import org.jetbrains.jet.j2k.ast.MethodCallExpression

public open class ExpressionVisitorForDirectObjectInheritors(converter: Converter) : ExpressionVisitor(converter) {
    public override fun visitMethodCallExpression(expression: PsiMethodCallExpression?) {
        val methodExpression = expression?.getMethodExpression()!!
        if (superMethodInvocation(methodExpression, "hashCode")) {
            myResult = MethodCallExpression.build(Identifier("System", false), "identityHashCode", arrayList(Identifier("this")))
        }
        else if (superMethodInvocation(methodExpression, "equals")) {
            myResult = MethodCallExpression.build(Identifier("this", false), "identityEquals", getConverter().argumentsToExpressionList(expression!!))
        }
        else if (superMethodInvocation(methodExpression, "toString")) {
            myResult = DummyStringExpression(java.lang.String.format("getJavaClass<%s>.getName() + '@' + Integer.toHexString(hashCode())",
                                                                     ExpressionVisitor.getClassName(methodExpression)))
        }
        else {
            convertMethodCallExpression(expression!!)
        }
    }

    class object {
        private fun superMethodInvocation(expression: PsiReferenceExpression, methodName: String?): Boolean {
            val referenceName: String? = expression.getReferenceName()
            val qualifierExpression: PsiExpression? = expression.getQualifierExpression()
            if (referenceName == methodName && qualifierExpression is PsiSuperExpression) {
                val `type`: PsiType? = qualifierExpression.getType()
                if (`type` != null && `type`.getCanonicalText() == JAVA_LANG_OBJECT) {
                    return true
                }
            }
            return false
        }
    }
}
