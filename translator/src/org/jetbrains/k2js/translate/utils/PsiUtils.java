package org.jetbrains.k2js.translate.utils;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetOperationExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lexer.JetToken;

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
}
