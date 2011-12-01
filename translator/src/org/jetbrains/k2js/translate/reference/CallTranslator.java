package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsNew;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.Intrinsic;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptorForCallExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.*;

/**
 * @author Talanov Pavel
 */
public final class CallTranslator extends AbstractTranslator {

    public static boolean isFunctionCall(@NotNull JetOperationExpression expression,
                                         @NotNull TranslationContext context) {
        DeclarationDescriptor descriptor = getDescriptorForReferenceExpression
                (context.bindingContext(), expression.getOperation());
        return (descriptor instanceof FunctionDescriptor);
    }

    public static JsExpression translate(@NotNull JetUnaryExpression unaryExpression,
                                         @NotNull TranslationContext context) {
        JsExpression receiver = TranslationUtils.translateBaseExpression(context, unaryExpression);
        List<JsExpression> arguments = Collections.emptyList();
        DeclarationDescriptor descriptor = getDescriptorForReferenceExpression
                (context.bindingContext(), unaryExpression.getOperation());
        assert descriptor instanceof FunctionDescriptor;
        return (new CallTranslator(receiver, arguments, (FunctionDescriptor) descriptor, context)).translate();
    }

    public static JsExpression translate(@NotNull JetDotQualifiedExpression dotExpression,
                                         @NotNull TranslationContext context) {
        //TODO: look for duplication
        JsExpression receiver = translateReceiver(context, dotExpression);
        JetExpression selectorExpression = dotExpression.getSelectorExpression();
        assert selectorExpression instanceof JetCallExpression;
        JetCallExpression callExpression = (JetCallExpression) selectorExpression;
        List<JsExpression> arguments =
                translateArgumentList(context, callExpression.getValueArguments());
        FunctionDescriptor descriptor =
                getFunctionDescriptorForCallExpression(context.bindingContext(), callExpression);
        return (new CallTranslator(receiver, arguments, descriptor, context)).translate();
    }

    public static JsExpression translate(@NotNull JetCallExpression callExpression,
                                         @NotNull TranslationContext context) {
        DeclarationDescriptor descriptor =
                getFunctionDescriptorForCallExpression(context.bindingContext(), callExpression);
        JsExpression receiver = ReferenceTranslator.getImplicitReceiver(descriptor, context);
        List<JsExpression> arguments = translateArgumentList(context, callExpression.getValueArguments());
        return (new CallTranslator(receiver, arguments, (FunctionDescriptor) descriptor, context)).translate();
    }

    public static JsExpression translate(@NotNull JetBinaryExpression binaryExpression,
                                         @NotNull TranslationContext context) {
        JsExpression receiver = translateLeftExpression(context, binaryExpression);
        List<JsExpression> arguments = Arrays.asList(translateRightExpression(context, binaryExpression));
        //TODO: use PSI util method to get operation reference
        DeclarationDescriptor descriptor = getDescriptorForReferenceExpression
                (context.bindingContext(), binaryExpression.getOperation());
        assert descriptor instanceof FunctionDescriptor;
        return (new CallTranslator(receiver, arguments, (FunctionDescriptor) descriptor, context)).translate();
    }

    @Nullable
    private final JsExpression receiver;

    @NotNull
    private final List<JsExpression> arguments;

    @NotNull
    private final FunctionDescriptor descriptor;

    private CallTranslator(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments,
                           @NotNull FunctionDescriptor descriptor, @NotNull TranslationContext context) {
        super(context);
        this.receiver = receiver;
        this.arguments = arguments;
        this.descriptor = descriptor;
    }

    @NotNull
    private JsExpression translate() {
        if (context().intrinsics().hasDescriptor(descriptor)) {
            Intrinsic intrinsic = context().intrinsics().getIntrinsic(descriptor);
            assert receiver != null : "Functions that have intrinsic implementation should have a receiver.";
            return intrinsic.apply(receiver, arguments, context());
        }
        if (isConstructorDescriptor(descriptor)) {
            return constructorCall();
        }
        return AstUtil.newInvocation(qualifiedMethodReference(), arguments);
    }

    @NotNull
    private JsNameRef qualifiedMethodReference() {
        JsName methodName = context().getNameForDescriptor(descriptor);
        if (receiver != null) {
            return AstUtil.qualified(methodName, receiver);
        }
        return methodName.makeRef();
    }

    @NotNull
    private JsExpression constructorCall() {
        JsNew constructorCall = new JsNew(qualifiedMethodReference());
        constructorCall.setArguments(arguments);
        return constructorCall;
    }

//    //TODO: delete?
//    @NotNull
//    private JsExpression translateCallee(@NotNull JetCallExpression expression) {
//        JetExpression callee = expression.getCalleeExpression();
//        assert callee != null : "Call expression with no callee encountered!";
//        return Translation.translateAsExpression(callee, context);
//    }


}
