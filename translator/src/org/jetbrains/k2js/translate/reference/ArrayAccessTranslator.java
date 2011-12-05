package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class ArrayAccessTranslator extends AccessTranslator {

    public static ArrayAccessTranslator newInstance(@NotNull JetArrayAccessExpression expression,
                                                    @NotNull TranslationContext context) {
        return new ArrayAccessTranslator(expression, context);
    }

    @NotNull
    private final JetArrayAccessExpression expression;
    @NotNull
    private final DeclarationDescriptor methodDescriptor;

    private ArrayAccessTranslator(@NotNull JetArrayAccessExpression expression,
                                  @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        DeclarationDescriptor descriptorForReferenceExpression =
                BindingUtils.getDescriptorForReferenceExpression(context.bindingContext(), expression);
        assert descriptorForReferenceExpression != null : "Array access expression must reference a descriptor";
        this.methodDescriptor = descriptorForReferenceExpression;
    }

    @Override
    @NotNull
    public JsInvocation translateAsGet() {
        return translateAsArrayAccessWithIndices();
    }

    @Override
    @NotNull
    public JsExpression translateAsSet(@NotNull JsExpression expression) {
        JsInvocation setCall = translateAsArrayAccessWithIndices();
        setCall.getArguments().add(expression);
        return setCall;
    }

    private JsInvocation translateAsArrayAccessWithIndices() {
        JsNameRef accessMethodReference = getAccessMethodReference();
        AstUtil.setQualifier(accessMethodReference, translateArrayExpression());
        return AstUtil.newInvocation(accessMethodReference, translateIndexExpressions());
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
