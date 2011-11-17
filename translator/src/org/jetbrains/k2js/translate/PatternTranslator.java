package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetIsExpression;
import org.jetbrains.jet.lang.psi.JetPattern;
import org.jetbrains.jet.lang.psi.JetTypePattern;
import org.jetbrains.jet.lang.psi.JetTypeReference;

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

//    @NotNull
//    public JsExpression translate(@NotNull JetBinaryExpressionWithTypeRHS expression) {
//        JsExpression left = AstUtil.convertToExpression
//                (Translation.translateExpression(expression.getLeft(), translationContext()));
//        JsNameRef right = getClassReference(expression);
//        JetToken token = getOperationToken(expression);
//        if (OperatorTable.hasCorrespondingFunctionInvocation(token)) {
//            JsInvocation functionInvocation = OperatorTable.getCorrespondingFunctionInvocation(token);
//            functionInvocation.setArguments(Arrays.asList(left, right));
//            return functionInvocation;
//        }
//        throw new AssertionError("Unsupported token encountered: " + token.toString());
//    }

    @NotNull
    public JsExpression translateIsExpression(@NotNull JetIsExpression expression) {
        JsExpression left = AstUtil.convertToExpression
                (Translation.translateExpression(expression.getLeftHandSide(), translationContext()));
        JetPattern pattern = getPattern(expression);
        JsExpression resultingExpression = translatePattern(pattern, left);
        if (expression.isNegated()) {
            return AstUtil.negation(resultingExpression);
        }
        return resultingExpression;
    }

    @NotNull
    public JsExpression translatePattern(@NotNull JetPattern pattern, @NotNull JsExpression expressionToMatch) {
        if (pattern instanceof JetTypePattern) {
            return translateTypePattern(expressionToMatch, (JetTypePattern) pattern);
        }
        throw new AssertionError("Unsupported pattern type " + pattern.getClass());
    }

    @NotNull
    private JetPattern getPattern(@NotNull JetIsExpression expression) {
        JetPattern pattern = expression.getPattern();
        assert pattern != null : "Pattern should not be null";
        return pattern;
    }

    @NotNull
    private JsExpression translateTypePattern(@NotNull JsExpression expressionToMatch,
                                              @NotNull JetTypePattern pattern) {
        return AstUtil.newInvocation(Namer.isOperationReference(), expressionToMatch, getClassReference(pattern));
    }

    @NotNull
    private JsExpression getClassReference(@NotNull JetTypePattern pattern) {
        JetTypeReference typeReference = pattern.getTypeReference();
        assert typeReference != null : "Type pattern should contain a type reference";
        return getClassNameReferenceForTypeReference(typeReference);
    }

//    @NotNull
//    private JsNameRef getClassReference(@NotNull JetBinaryExpressionWithTypeRHS expression) {
//        JetTypeReference typeReference = expression.getRight();
//        assert typeReference != null : "Binary type expression should have a right expression";
//        return getClassNameReferenceForTypeReference(typeReference);
//    }

    @NotNull
    private JsNameRef getClassNameReferenceForTypeReference(@NotNull JetTypeReference typeReference) {
        ClassDescriptor referencedClass = BindingUtils.getClassDescriptorForTypeReference
                (translationContext().bindingContext(), typeReference);
        //TODO should reference class by full name here
        JsName className = translationContext().getNameForDescriptor(referencedClass);
        return translationContext().getNamespaceQualifiedReference(className);
    }

//    @NotNull
//    private JetToken getOperationToken(@NotNull JetBinaryExpressionWithTypeRHS expression) {
//        JetSimpleNameExpression operationExpression = expression.getOperationSign();
//        IElementType elementType = operationExpression.getReferencedNameElementType();
//        assert elementType instanceof JetToken : "Binary type operation should have IElementType of type JetToken";
//        return (JetToken) elementType;
//    }


}
