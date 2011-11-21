package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author Talanov Pavel
 */
public final class TranslationUtils {

    @NotNull
    static public JsBinaryOperation notNullCheck(@NotNull TranslationContext context,
                                                 @NotNull JsExpression expressionToCheck) {
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        return new JsBinaryOperation
                (JsBinaryOperator.NEQ, expressionToCheck, nullLiteral);
    }

    @NotNull
    static public JsBinaryOperation isNullCheck(@NotNull TranslationContext context,
                                                @NotNull JsExpression expressionToCheck) {
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        return new JsBinaryOperation
                (JsBinaryOperator.REF_EQ, expressionToCheck, nullLiteral);
    }

    //TODO: make logic clear
    @NotNull
    static public JsNameRef generateCorrectReference(@NotNull TranslationContext context,
                                                     @NotNull JetSimpleNameExpression expression,
                                                     @NotNull JsName referencedName) {
        if (requiresNamespaceQualifier(context, referencedName)) {
            return context.getNamespaceQualifiedReference(referencedName);
        } else if (requiresThisQualifier(context, expression, referencedName)) {
            return AstUtil.thisQualifiedReference(referencedName);
        }
        return referencedName.makeRef();
    }

    static private boolean requiresNamespaceQualifier(@NotNull TranslationContext context,
                                                      @NotNull JsName referencedName) {
        return context.namespaceScope().ownsName(referencedName);
    }

    static private boolean requiresThisQualifier(@NotNull TranslationContext context,
                                                 @NotNull JetSimpleNameExpression expression,
                                                 @NotNull JsName referencedName) {
        JsName name = context.enclosingScope().findExistingName(referencedName.getIdent());
        boolean isClassMember = context.classScope().ownsName(name);
        boolean isBackingFieldAccess = expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER;
        return isClassMember || isBackingFieldAccess;
    }

    @Nullable
    static public JsName getLocalReferencedName(@NotNull TranslationContext context,
                                                @NotNull JetSimpleNameExpression expression) {
        String referencedName = expression.getReferencedName();
        return context.enclosingScope().findExistingName(referencedName);
    }

}
