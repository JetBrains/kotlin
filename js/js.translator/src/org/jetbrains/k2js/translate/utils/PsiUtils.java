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

package org.jetbrains.k2js.translate.utils;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

public final class PsiUtils {

    private PsiUtils() {
    }

    @Nullable
    public static JetSimpleNameExpression getSelectorAsSimpleName(@NotNull JetQualifiedExpression expression) {
        JetExpression selectorExpression = getSelector(expression);
        if (!(selectorExpression instanceof JetSimpleNameExpression)) {
            return null;
        }
        return (JetSimpleNameExpression) selectorExpression;
    }

    @NotNull
    public static JetExpression getSelector(@NotNull JetQualifiedExpression expression) {
        JetExpression selectorExpression = expression.getSelectorExpression();
        assert selectorExpression != null : "Selector should not be null.";
        return selectorExpression;
    }

    @NotNull
    public static JetSimpleNameExpression getNotNullSimpleNameSelector(@NotNull JetQualifiedExpression expression) {
        JetSimpleNameExpression selectorAsSimpleName = getSelectorAsSimpleName(expression);
        assert selectorAsSimpleName != null;
        return selectorAsSimpleName;
    }

    @NotNull
    public static JetToken getOperationToken(@NotNull JetOperationExpression expression) {
        JetSimpleNameExpression operationExpression = expression.getOperationReference();
        IElementType elementType = operationExpression.getReferencedNameElementType();
        assert elementType instanceof JetToken : "Unary expression should have operation token of type JetToken";
        return (JetToken) elementType;
    }

    @NotNull
    public static JetExpression getBaseExpression(@NotNull JetUnaryExpression expression) {
        JetExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null;
        return baseExpression;
    }

    public static boolean isPrefix(@NotNull JetUnaryExpression expression) {
        return (expression instanceof JetPrefixExpression);
    }

    public static boolean isAssignment(JetToken token) {
        return (token == JetTokens.EQ);
    }

    public static boolean isInOrNotInOperation(@NotNull JetBinaryExpression binaryExpression) {
        return isInOperation(binaryExpression) || isNotInOperation(binaryExpression);
    }

    public static boolean isNotInOperation(@NotNull JetBinaryExpression binaryExpression) {
        return (binaryExpression.getOperationToken() == JetTokens.NOT_IN);
    }

    public static boolean isNegatedOperation(@NotNull JetBinaryExpression binaryExpression) {
        return (binaryExpression.getOperationToken() == JetTokens.EXCLEQ) || isNotInOperation(binaryExpression);
    }

    private static boolean isInOperation(@NotNull JetBinaryExpression binaryExpression) {
        return (binaryExpression.getOperationToken() == JetTokens.IN_KEYWORD);
    }

    @NotNull
    public static JetExpression getLoopBody(@NotNull JetLoopExpression expression) {
        JetExpression body = expression.getBody();
        assert body != null : "Loops cannot have null bodies.";
        return body;
    }

    @Nullable
    public static JetParameter getLoopParameter(@NotNull JetForExpression expression) {
        return expression.getLoopParameter();
    }

    @NotNull
    public static List<JetParameter> getPrimaryConstructorParameters(@NotNull JetClassOrObject classDeclaration) {
        if (classDeclaration instanceof JetClass) {
            return ((JetClass) classDeclaration).getPrimaryConstructorParameters();
        }
        return Collections.emptyList();
    }

    @NotNull
    public static JetExpression getLoopRange(@NotNull JetForExpression expression) {
        JetExpression rangeExpression = expression.getLoopRange();
        assert rangeExpression != null;
        return rangeExpression;
    }
}
