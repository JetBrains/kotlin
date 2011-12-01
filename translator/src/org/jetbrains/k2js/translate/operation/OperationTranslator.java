package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetOperationExpression;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.CompareToIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.EqualsIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.Intrinsic;
import org.jetbrains.k2js.translate.reference.ArrayAccessTranslator;
import org.jetbrains.k2js.translate.reference.CallTranslator;
import org.jetbrains.k2js.translate.reference.PropertyAccessTranslator;

import java.util.Arrays;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateLeftExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateRightExpression;

/**
 * @author Talanov Pavel
 */
public class OperationTranslator extends AbstractTranslator {

    @NotNull
    public static JsExpression translate(@NotNull JetBinaryExpression expression,
                                         @NotNull TranslationContext context) {
        //TODO: move to assignment translator
        if (ArrayAccessTranslator.canBeArraySetterCall(expression)) {
            return ArrayAccessTranslator.translateAsArraySetterCall(expression, context);
        }
        DeclarationDescriptor descriptorForReferenceExpression = getDescriptorForReferenceExpression
                (context.bindingContext(), expression.getOperation());
        if (descriptorForReferenceExpression == null) {
            return IntrinsicBinaryOperationTranslator.translate(expression, context);
        }
        if (isEqualsCall(expression, context)) {
            return translateAsEqualsCall(expression, context);
        }
        if (isCompareToCall(expression, context)) {
            return translateAsCompareToCall(expression, context);
        }
        return CallTranslator.translate(expression, context);
    }

    @NotNull
    public static JsExpression translate(@NotNull JetUnaryExpression expression,
                                         @NotNull TranslationContext context) {
        return CallTranslator.translate(expression, context);
    }


    public static boolean isEqualsCall(@NotNull JetBinaryExpression expression,
                                       @NotNull TranslationContext context) {
        DeclarationDescriptor operationDescriptor = getOperationDescriptor(expression, context);
        //TODO: add descriptor is equals utils
        boolean isEquals = operationDescriptor.getName().equals("equals");
        boolean isIntrinsic = context.intrinsics().hasDescriptor(operationDescriptor);
        return isEquals && isIntrinsic;
    }

    @NotNull
    public static JsExpression translateAsEqualsCall(@NotNull JetBinaryExpression expression,
                                                     @NotNull TranslationContext context) {
        Intrinsic intrinsic = context.intrinsics().
                getIntrinsic(getOperationDescriptor(expression, context));
        //TODO
        ((EqualsIntrinsic) intrinsic).setNegated(expression.getOperationToken().equals(JetTokens.EXCLEQ));
        return intrinsic.apply(translateLeftExpression(context, expression),
                Arrays.asList(translateRightExpression(context, expression)), context);
    }

    public static boolean isCompareToCall(@NotNull JetBinaryExpression expression,
                                          @NotNull TranslationContext context) {
        DeclarationDescriptor operationDescriptor = getOperationDescriptor(expression, context);
        return (operationDescriptor.getName().equals("compareTo")
                && (context.intrinsics().hasDescriptor(operationDescriptor)));
    }

    @NotNull
    public static JsExpression translateAsCompareToCall(@NotNull JetBinaryExpression expression,
                                                        @NotNull TranslationContext context) {
        Intrinsic intrinsic = context.intrinsics().
                getIntrinsic(getOperationDescriptor(expression, context));
        ((CompareToIntrinsic) intrinsic).setComparisonToken((JetToken) expression.getOperationToken());
        return intrinsic.apply(translateLeftExpression(context, expression),
                Arrays.asList(translateRightExpression(context, expression)), context);
    }

    protected OperationTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    //TODO: refactor
    @Nullable
    protected static JsNameRef getOverloadedOperationReference(@NotNull JetOperationExpression operationExpression,
                                                               @NotNull TranslationContext context) {
        DeclarationDescriptor operationDescriptor = getOperationDescriptor(operationExpression, context);
        if (!context.isDeclared(operationDescriptor)) {
            return null;
        }
        return context.getNameForDescriptor(operationDescriptor).makeRef();
    }

    @NotNull
    private static DeclarationDescriptor getOperationDescriptor(@NotNull JetOperationExpression expression,
                                                                @NotNull TranslationContext context) {
        DeclarationDescriptor descriptorForReferenceExpression = getDescriptorForReferenceExpression
                (context.bindingContext(), expression.getOperation());
        assert descriptorForReferenceExpression != null;
        return descriptorForReferenceExpression;
    }

    protected boolean isPropertyAccess(@NotNull JetExpression expression) {
        return PropertyAccessTranslator.canBePropertyAccess(expression, context());

    }

    protected final class TemporaryVariable {

        @NotNull
        private final JsExpression assignmentExpression;
        @NotNull
        private final JsName variableName;

        private TemporaryVariable(@NotNull JsExpression initExpression) {
            this.variableName = context().enclosingScope().declareTemporary();
            this.assignmentExpression = AstUtil.newAssignment(variableName.makeRef(), initExpression);
        }

        @NotNull
        public JsNameRef nameReference() {
            return variableName.makeRef();
        }

        @NotNull
        public JsExpression assignmentExpression() {
            return assignmentExpression;
        }
    }

    @NotNull
    public TemporaryVariable declareTemporary(@NotNull JsExpression initExpression) {
        return new TemporaryVariable(initExpression);
    }
}
