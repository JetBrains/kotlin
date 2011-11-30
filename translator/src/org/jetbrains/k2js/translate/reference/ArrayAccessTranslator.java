package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.operation.OperatorTable;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class ArrayAccessTranslator extends AbstractTranslator {


    public static boolean canBeArraySetterCall(@NotNull JetBinaryExpression expression) {
        //TODO: move unsafe call to util
        return (OperatorTable.isAssignment((JetToken) expression.getOperationToken())
                && (expression.getLeft() instanceof JetArrayAccessExpression));
    }

    public static JsInvocation translateAsArrayGetterCall(@NotNull JetArrayAccessExpression expression,
                                                          @NotNull TranslationContext context) {
        return (new ArrayAccessTranslator(expression, null, context)).translateAsArrayGet();
    }

    public static JsInvocation translateAsArraySetterCall(@NotNull JetBinaryExpression expression,
                                                          @NotNull TranslationContext context) {
        JetExpression arrayAccess = expression.getLeft();
        assert (arrayAccess instanceof JetArrayAccessExpression) : "Check with canBeArraySetterCall";
        JetExpression right = expression.getRight();
        assert right != null : "Binary expression should have a right expression";
        JsExpression expressionToSetTo = Translation.translateAsExpression(right, context);
        return (new ArrayAccessTranslator((JetArrayAccessExpression) arrayAccess, expressionToSetTo, context))
                .translateAsArraySet();
    }

    @NotNull
    private final JetArrayAccessExpression expression;
    @NotNull
    private final DeclarationDescriptor methodDescriptor;
    @Nullable
    private final JsExpression expressionToSetTo;


    private ArrayAccessTranslator(@NotNull JetArrayAccessExpression expression,
                                  @Nullable JsExpression expressionToSetTo,
                                  @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        DeclarationDescriptor descriptorForReferenceExpression =
                BindingUtils.getDescriptorForReferenceExpression(context.bindingContext(), expression);
        assert descriptorForReferenceExpression != null : "Array access expression must reference a descriptor";
        this.methodDescriptor = descriptorForReferenceExpression;
        this.expressionToSetTo = expressionToSetTo;
    }

    @NotNull
    private JsInvocation translateAsArrayGet() {
        return translateAsArrayAccessWithIndices();
    }

    private JsInvocation translateAsArrayAccessWithIndices() {
        JsNameRef accessMethodReference = getAccessMethodReference();
        AstUtil.setQualifier(accessMethodReference, translateArrayExpression());
        return AstUtil.newInvocation(accessMethodReference, translateIndexExpressions());
    }

    @NotNull
    private JsInvocation translateAsArraySet() {
        assert expressionToSetTo != null;
        JsInvocation setCall = translateAsArrayAccessWithIndices();
        setCall.getArguments().add(expressionToSetTo);
        return setCall;
    }

    @NotNull
    private List<JsExpression> translateIndexExpressions() {
        return TranslationUtils.translateExpressionList(context(), expression.getIndexExpressions());
    }

    @NotNull
    private JsNameRef getAccessMethodReference() {
        return context().getNameForDescriptor(methodDescriptor).makeRef();
    }

    @NotNull
    private JsExpression translateArrayExpression() {
        return Translation.translateAsExpression(expression.getArrayExpression(), context());
    }


}
