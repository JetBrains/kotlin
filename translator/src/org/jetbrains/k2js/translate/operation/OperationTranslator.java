package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.reference.PropertyAccessTranslator;
import org.jetbrains.k2js.translate.utils.BindingUtils;

/**
 * @author Talanov Pavel
 */
public class OperationTranslator extends AbstractTranslator {

    protected OperationTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @Nullable
    protected JsNameRef getOverloadedOperationReference(@NotNull JetSimpleNameExpression operationExpression) {
        DeclarationDescriptor operationDescriptor = getOperationDescriptor(operationExpression);
        if (operationDescriptor == null) {
            return null;
        }
        if (context().intrinsics().isIntrinsic(operationDescriptor)) {
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
