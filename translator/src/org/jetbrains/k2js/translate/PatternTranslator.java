package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.*;

/**
 * @author Talanov Pavel
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
        JsExpression left = Translation.translateAsExpression(expression.getLeftHandSide(), translationContext());
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

        JsInvocation isCheck = AstUtil.newInvocation(Namer.isOperationReference(),
                expressionToMatch, getClassReference(pattern));
        if (isNullable(pattern)) {
            return addNullCheck(expressionToMatch, isCheck);
        }
        return isCheck;
    }

    @NotNull
    private JsExpression addNullCheck(@NotNull JsExpression expressionToMatch, @NotNull JsInvocation isCheck) {
        return AstUtil.or(TranslationUtils.isNullCheck(expressionToMatch, translationContext()), isCheck);
    }

    private boolean isNullable(JetTypePattern pattern) {
        return BindingUtils.getTypeByReference(translationContext().bindingContext(),
                getTypeReference(pattern)).isNullable();
    }

    @NotNull
    private JsExpression getClassReference(@NotNull JetTypePattern pattern) {
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
                (translationContext().bindingContext(), typeReference);
        //TODO should reference class by full name here
        JsName className = translationContext().getNameForDescriptor(referencedClass);
        return translationContext().getNamespaceQualifiedReference(className);
    }

    @NotNull
    private JsExpression translateExpressionPattern(JsExpression expressionToMatch, JetExpressionPattern pattern) {
        JetExpression patternExpression = pattern.getExpression();
        assert patternExpression != null : "Expression patter should have an expression.";
        JsExpression expressionToMatchAgainst =
                Translation.translateAsExpression(patternExpression, translationContext());
        //TODO: should call equals method here
        return new JsBinaryOperation(JsBinaryOperator.REF_EQ, expressionToMatch, expressionToMatchAgainst);
    }
}
