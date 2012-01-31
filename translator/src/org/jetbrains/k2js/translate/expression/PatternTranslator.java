package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

/**
 * @author Pavel Talanov
 */
public final class PatternTranslator extends AbstractTranslator {

    @NotNull
    public static PatternTranslator newInstance(@NotNull TranslationContext context) {
        return new PatternTranslator(context);
    }

    private PatternTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @NotNull
    public JsExpression translateIsExpression(@NotNull JetIsExpression expression) {
        JsExpression left = Translation.translateAsExpression(expression.getLeftHandSide(), context());
        JetPattern pattern = getPattern(expression);
        JsExpression resultingExpression = translatePattern(pattern, left);
        if (expression.isNegated()) {
            return AstUtil.negated(resultingExpression);
        }
        return resultingExpression;
    }

    @NotNull
    private JetPattern getPattern(@NotNull JetIsExpression expression) {
        JetPattern pattern = expression.getPattern();
        assert pattern != null : "Pattern should not be null";
        return pattern;
    }

    @NotNull
    public JsExpression translatePattern(@NotNull JetPattern pattern, @NotNull JsExpression expressionToMatch) {
        if (pattern instanceof JetTypePattern) {
            return translateTypePattern(expressionToMatch, (JetTypePattern) pattern);
        }
        if (pattern instanceof JetExpressionPattern) {
            return translateExpressionPattern(expressionToMatch, (JetExpressionPattern) pattern);
        }
        throw new AssertionError("Unsupported pattern type " + pattern.getClass());
    }

    @NotNull
    private JsExpression translateTypePattern(@NotNull JsExpression expressionToMatch,
                                              @NotNull JetTypePattern pattern) {
        //TODO: look into using intrinsics or other mechanisms to implement this logic
        JsName className = getClassReference(pattern).getName();
        if (className.getIdent().equals("String")) {
            return AstUtil.typeof(expressionToMatch, program().getStringLiteral("string"));
        }
        if (className.getIdent().equals("Int")) {
            return AstUtil.typeof(expressionToMatch, program().getStringLiteral("number"));
        }

        JsInvocation isCheck = AstUtil.newInvocation(context().namer().isOperationReference(),
                expressionToMatch, getClassReference(pattern));
        if (isNullable(pattern)) {
            return addNullCheck(expressionToMatch, isCheck);
        }
        return isCheck;
    }

    @NotNull
    private JsExpression addNullCheck(@NotNull JsExpression expressionToMatch, @NotNull JsInvocation isCheck) {
        return AstUtil.or(TranslationUtils.isNullCheck(context(), expressionToMatch), isCheck);
    }

    private boolean isNullable(JetTypePattern pattern) {
        return BindingUtils.getTypeByReference(context().bindingContext(),
                getTypeReference(pattern)).isNullable();
    }

    @NotNull
    private JsNameRef getClassReference(@NotNull JetTypePattern pattern) {
        JetTypeReference typeReference = getTypeReference(pattern);
        return getClassNameReferenceForTypeReference(typeReference);
    }

    @NotNull
    private JetTypeReference getTypeReference(@NotNull JetTypePattern pattern) {
        JetTypeReference typeReference = pattern.getTypeReference();
        assert typeReference != null : "Type pattern should contain a type reference";
        return typeReference;
    }

    @NotNull
    private JsNameRef getClassNameReferenceForTypeReference(@NotNull JetTypeReference typeReference) {
        ClassDescriptor referencedClass = BindingUtils.getClassDescriptorForTypeReference
                (context().bindingContext(), typeReference);
        return TranslationUtils.getQualifiedReference(context(), referencedClass);
    }

    @NotNull
    private JsExpression translateExpressionPattern(JsExpression expressionToMatch, JetExpressionPattern pattern) {
        JetExpression patternExpression = pattern.getExpression();
        assert patternExpression != null : "Expression patter should have an expression.";
        JsExpression expressionToMatchAgainst =
                Translation.translateAsExpression(patternExpression, context());
        return AstUtil.equals(expressionToMatch, expressionToMatchAgainst);
    }
}
