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

open class ExpressionVisitorForDirectObjectInheritors(converter: Converter, usageReplacementMap: Map<PsiVariable, String> = mapOf())
: ExpressionVisitor(converter, usageReplacementMap) {
    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        val methodExpression = expression.getMethodExpression()
        if (isSuperMethodInvocation(methodExpression, "hashCode")) {
            result = MethodCallExpression.build(Identifier("System", false), "identityHashCode", listOf(Identifier("this")))
        }
        else if (isSuperMethodInvocation(methodExpression, "equals")) {
            result = MethodCallExpression.build(Identifier("this", false), "identityEquals", converter.convertArguments(expression))
        }
        else if (isSuperMethodInvocation(methodExpression, "toString")) {
            result = DummyStringExpression("getJavaClass<${getClassName(methodExpression)}>.getName() + '@' + Integer.toHexString(hashCode())")
        }
        else {
            convertMethodCallExpression(expression)
        }
    }

    private fun isSuperMethodInvocation(expression: PsiReferenceExpression, methodName: String): Boolean
            = expression.getReferenceName() == methodName
                && (expression.getQualifierExpression() as? PsiSuperExpression)?.getType()?.getCanonicalText() == JAVA_LANG_OBJECT
}
