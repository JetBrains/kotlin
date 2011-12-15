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
import org.jetbrains.k2js.translate.intrinsic.array.ArrayGetIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.array.ArraySetIntrinsic;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;

/**
 * @author Pavel Talanov
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
        this.methodDescriptor = getDescriptorForReferenceExpression(context.bindingContext(), expression);
    }

    @Override
    @NotNull
    public JsExpression translateAsGet() {
        if (intrinsicCall()) {
            return intrinsicGet();
        }
        return translateAsAccessMethodCall();
    }

    private boolean intrinsicCall() {
        return context().intrinsics().isIntrinsic(methodDescriptor);
    }

    @NotNull
    private JsExpression intrinsicGet() {
        return ArrayGetIntrinsic.INSTANCE.apply(translateArrayExpression(), translateIndexExpressions(), context());
    }

    @Override
    @NotNull
    public JsExpression translateAsSet(@NotNull JsExpression expression) {
        if (intrinsicCall()) {
            return intrinsicSet(expression);
        }
        return overloadedSet(expression);
    }

    @NotNull
    private JsExpression intrinsicSet(@NotNull JsExpression expression) {
        List<JsExpression> arguments = translateIndexExpressions();
        arguments.add(expression);
        return ArraySetIntrinsic.INSTANCE.apply(translateArrayExpression(), arguments, context());
    }

    @NotNull
    private JsExpression overloadedSet(@NotNull JsExpression expression) {
        JsInvocation setCall = translateAsAccessMethodCall();
        setCall.getArguments().add(expression);
        return setCall;
    }

    @NotNull
    private JsInvocation translateAsAccessMethodCall() {
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
