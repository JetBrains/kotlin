package org.jetbrains.k2js.translate;

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
import org.jetbrains.jet.lang.psi.JetUnaryExpression;

/**
 * @author Talanov Pavel
 */
public class OperationTranslator extends AbstractTranslator {

    protected OperationTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @Nullable
    protected JsNameRef getOverloadedOperationReference(@NotNull JetExpression expression) {
        DeclarationDescriptor operationDescriptor = getOperationDescriptor(expression);
        if (operationDescriptor == null) {
            return null;
        }
        if (!translationContext().isDeclared(operationDescriptor)) {
            return null;
        }
        return translationContext().getNameForDescriptor(operationDescriptor).makeRef();
    }

    @Nullable
    private DeclarationDescriptor getOperationDescriptor(@NotNull JetExpression expression) {
        JetSimpleNameExpression operationReference = null;
        if (expression instanceof JetBinaryExpression) {
            operationReference = ((JetBinaryExpression) expression).getOperationReference();
        }
        if (expression instanceof JetUnaryExpression) {
            operationReference = ((JetUnaryExpression) expression).getOperationSign();
        }
        assert operationReference != null : "Should be applied only to unary or binary operations";
        return BindingUtils.getDescriptorForReferenceExpression
                (translationContext().bindingContext(), operationReference);
    }


    protected final class TemporaryVariable {

        @NotNull
        private final JsExpression assignmentExpression;
        @NotNull
        private final JsName variableName;

        private TemporaryVariable(@NotNull JsExpression initExpression) {
            this.variableName = translationContext().enclosingScope().declareTemporary();
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
