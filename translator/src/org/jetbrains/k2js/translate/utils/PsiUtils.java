package org.jetbrains.k2js.translate.utils;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author Talanov Pavel
 */
public final class PsiUtils {

    private PsiUtils() {
    }

    @Nullable
    public static JetSimpleNameExpression getSelectorAsSimpleName(@NotNull JetQualifiedExpression expression) {
        JetExpression selectorExpression = expression.getSelectorExpression();
        assert selectorExpression != null : "Selector should not be null.";
        if (!(selectorExpression instanceof JetSimpleNameExpression)) {
            return null;
        }
        return (JetSimpleNameExpression) selectorExpression;
    }

    @NotNull
    public static JetToken getOperationToken(@NotNull JetOperationExpression expression) {
        JetSimpleNameExpression operationExpression = expression.getOperation();
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
}
