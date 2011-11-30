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
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.intrinsic.Intrinsic;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.reference.ArrayAccessTranslator;
import org.jetbrains.k2js.translate.reference.PropertyAccessTranslator;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

/**
 * @author Talanov Pavel
 */
public class OperationTranslator extends AbstractTranslator {

    @NotNull
    static public JsExpression translate(@NotNull JetBinaryExpression expression,
                                         @NotNull TranslationContext context) {
        JsExpression result = tryToApplyIntrinsic(expression, context);
        if (result != null) return result;

        if (ArrayAccessTranslator.canBeArraySetterCall(expression)) {
            return ArrayAccessTranslator.translateAsArraySetterCall(expression, context);
        }
        return translateAsBinary(expression, context);
    }

    @NotNull
    private static JsExpression translateAsBinary(@NotNull JetBinaryExpression expression,
                                                  @NotNull TranslationContext context) {
        if (TranslationUtils.isIntrinsicOperation(context, expression.getOperationReference())) {
            return IntrinsicBinaryOperationTranslator.translate(expression, context);
        } else {
            return OverloadedBinaryOperationTranslator.translate(expression, context);
        }
    }

    @Nullable
    private static JsExpression tryToApplyIntrinsic(@NotNull JetBinaryExpression expression,
                                                    @NotNull TranslationContext context) {
        DeclarationDescriptor descriptor = BindingUtils.getDescriptorForReferenceExpression
                (context.bindingContext(), expression.getOperationReference());
        if (descriptor == null) {
            return null;
        }
        if (!context.intrinsics().hasDescriptor(descriptor)) {
            return null;
        }
        //TODO: should be no nulls here
        Intrinsic intrinsic = context.intrinsics().getIntrinsic(descriptor);
        if (intrinsic == null) return null;
        return intrinsic.apply(expression, context);
    }

    protected OperationTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @Nullable
    protected JsNameRef getOverloadedOperationReference(@NotNull JetSimpleNameExpression operationExpression) {
        DeclarationDescriptor operationDescriptor = getOperationDescriptor(operationExpression);
        if (operationDescriptor == null) {
            return null;
        }
        if (context().intrinsics().hasDescriptor(operationDescriptor)) {
            return null;
        }
        if (!context().isDeclared(operationDescriptor)) {
            return null;
        }
        return context().getNameForDescriptor(operationDescriptor).makeRef();
    }

    @Nullable
    private DeclarationDescriptor getOperationDescriptor(@NotNull JetSimpleNameExpression expression) {
        return BindingUtils.getDescriptorForReferenceExpression
                (context().bindingContext(), expression);
    }

    protected boolean isPropertyAccess(@NotNull JetExpression expression) {
        return PropertyAccessTranslator.canBePropertyAccess(expression, context());

    }

    protected boolean isVariableReassignment(@NotNull JetExpression expression) {
        return BindingUtils.isVariableReassignment
                (context().bindingContext(), expression);
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
