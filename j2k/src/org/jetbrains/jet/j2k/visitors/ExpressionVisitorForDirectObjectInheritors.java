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

package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.ast.DummyMethodCallExpression;
import org.jetbrains.jet.j2k.ast.DummyStringExpression;
import org.jetbrains.jet.j2k.ast.IdentifierImpl;

import static org.jetbrains.jet.j2k.visitors.TypeVisitor.JAVA_LANG_OBJECT;

public class ExpressionVisitorForDirectObjectInheritors extends ExpressionVisitor {
    public ExpressionVisitorForDirectObjectInheritors(@NotNull Converter converter) {
        super(converter);
    }

    @Override
    public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
        if (superMethodInvocation(expression.getMethodExpression(), "hashCode")) {
            myResult = new DummyMethodCallExpression(new IdentifierImpl("System"), "identityHashCode", new IdentifierImpl("this"));
        }
        else if (superMethodInvocation(expression.getMethodExpression(), "equals")) {
            myResult = new DummyMethodCallExpression(new IdentifierImpl("this"), "identityEquals", getConverter().elementToElement(expression.getArgumentList()));
        }
        else if (superMethodInvocation(expression.getMethodExpression(), "toString")) {
            myResult = new DummyStringExpression(String.format("getJavaClass<%s>.getName() + '@' + Integer.toHexString(hashCode())", getClassName(expression.getMethodExpression())));
        }
        else {
            super.visitMethodCallExpression(expression);
        }
    }

    @Override
    public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
        super.visitReferenceExpression(expression);
    }

    private static boolean superMethodInvocation(@NotNull PsiReferenceExpression expression, String methodName) {
        String referenceName = expression.getReferenceName();
        PsiExpression qualifierExpression = expression.getQualifierExpression();
        if (referenceName != null && referenceName.equals(methodName)) {
            if (qualifierExpression instanceof PsiSuperExpression) {
                PsiType type = qualifierExpression.getType();
                if (type != null && type.getCanonicalText().equals(JAVA_LANG_OBJECT)) {
                    return true;
                }
            }
        }
        return false;
    }
}
