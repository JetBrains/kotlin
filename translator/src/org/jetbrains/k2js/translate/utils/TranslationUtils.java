package org.jetbrains.k2js.translate.utils;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.NamingScope;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.*;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getExpectedReceiverDescriptor;

/**
 * @author Pavel Talanov
 */
public final class TranslationUtils {

    @NotNull
    public static JsBinaryOperation notNullCheck(@NotNull TranslationContext context,
                                                 @NotNull JsExpression expressionToCheck) {
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        return AstUtil.notEqual(expressionToCheck, nullLiteral);
    }

    @NotNull
    public static JsBinaryOperation isNullCheck(@NotNull TranslationContext context,
                                                @NotNull JsExpression expressionToCheck) {
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        return AstUtil.equals(expressionToCheck, nullLiteral);
    }

    @NotNull
    public static List<JsExpression> translateArgumentList(@NotNull TranslationContext context,
                                                           @NotNull List<? extends ValueArgument> jetArguments) {
        List<JsExpression> jsArguments = new ArrayList<JsExpression>();
        for (ValueArgument argument : jetArguments) {
            jsArguments.add(translateArgument(context, argument));
        }
        return jsArguments;
    }

    @NotNull
    private static JsExpression translateArgument(@NotNull TranslationContext context, @NotNull ValueArgument argument) {
        JetExpression jetExpression = argument.getArgumentExpression();
        if (jetExpression == null) {
            throw new AssertionError("Argument with no expression encountered!");
        }
        return Translation.translateAsExpression(jetExpression, context);
    }

    //TODO: refactor backing field reference generation to use the generic way
    @NotNull
    public static JsNameRef backingFieldReference(@NotNull TranslationContext context,
                                                  @NotNull JetProperty expression) {
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(context.bindingContext(), expression);
        return backingFieldReference(context, propertyDescriptor);
    }

    @NotNull
    public static JsNameRef backingFieldReference(@NotNull TranslationContext context,
                                                  @NotNull PropertyDescriptor descriptor) {
        JsName backingFieldName = context.getNameForDescriptor(descriptor);
        if (isOwnedByClass(descriptor)) {
            return AstUtil.qualified(backingFieldName, new JsThisRef());
        }
        assert isOwnedByNamespace(descriptor)
                : "Only classes and namespaces may own backing fields.";
        JsNameRef qualifier = context.getQualifierForDescriptor(descriptor);
        return AstUtil.qualified(backingFieldName, qualifier);
    }

    @Nullable
    public static JsExpression translateInitializerForProperty(@NotNull JetProperty declaration,
                                                               @NotNull TranslationContext context) {
        JsExpression jsInitExpression = null;
        JetExpression initializer = declaration.getInitializer();
        if (initializer != null) {
            jsInitExpression = Translation.translateAsExpression(initializer, context);
        }
        return jsInitExpression;
    }

    @NotNull
    public static JsStatement assignmentToBackingFieldFromParameter(@NotNull TranslationContext context,
                                                                    @NotNull PropertyDescriptor descriptor,
                                                                    @NotNull JsParameter parameter) {
        JsNameRef backingFieldReference = backingFieldReference(context, descriptor);
        return AstUtil.newAssignmentStatement(backingFieldReference, parameter.getName().makeRef());
    }

    @NotNull
    public static JsNameRef getQualifiedReference(@NotNull TranslationContext context,
                                                  @NotNull DeclarationDescriptor descriptor) {
        JsName name = context.declarations().getName(descriptor);
        JsNameRef reference = name.makeRef();
        if (context.hasQualifierForDescriptor(descriptor)) {
            JsNameRef qualifier = context.getQualifierForDescriptor(descriptor);
            AstUtil.setQualifier(reference, qualifier);
        }
        return reference;
    }

    //TODO: refactor
    @NotNull
    public static JsExpression getThisObject(@NotNull TranslationContext context,
                                             @NotNull DeclarationDescriptor correspondingDeclaration) {
        JsExpression thisRef = null;
        if (correspondingDeclaration instanceof ClassDescriptor) {
            if (context.aliaser().hasAliasForThis(correspondingDeclaration)) {
                thisRef = context.aliaser().getAliasForThis(correspondingDeclaration);
            }
        }
        if (correspondingDeclaration instanceof FunctionDescriptor) {
            DeclarationDescriptor receiverDescriptor = getExpectedReceiverDescriptor((FunctionDescriptor) correspondingDeclaration);
            assert receiverDescriptor != null;
            if (context.aliaser().hasAliasForThis(receiverDescriptor)) {
                thisRef = context.aliaser().getAliasForThis(receiverDescriptor);
            }
        }
        if (thisRef != null) {
            return thisRef;
        }
        return new JsThisRef();
    }

    @NotNull
    public static List<JsExpression> translateExpressionList(@NotNull TranslationContext context,
                                                             @NotNull List<JetExpression> expressions) {
        List<JsExpression> result = new ArrayList<JsExpression>();
        for (JetExpression expression : expressions) {
            result.add(Translation.translateAsExpression(expression, context));
        }
        return result;
    }

    @NotNull
    public static JsExpression translateBaseExpression(@NotNull TranslationContext context,
                                                       @NotNull JetUnaryExpression expression) {
        JetExpression baseExpression = PsiUtils.getBaseExpression(expression);
        return Translation.translateAsExpression(baseExpression, context);
    }

    @NotNull
    public static JsExpression translateReceiver(@NotNull TranslationContext context,
                                                 @NotNull JetDotQualifiedExpression expression) {
        return Translation.translateAsExpression(expression.getReceiverExpression(), context);
    }


    @NotNull
    public static JsExpression translateLeftExpression(@NotNull TranslationContext context,
                                                       @NotNull JetBinaryExpression expression) {
        return Translation.translateAsExpression(expression.getLeft(), context);
    }

    @NotNull
    public static JsExpression translateRightExpression(@NotNull TranslationContext context,
                                                        @NotNull JetBinaryExpression expression) {
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Binary expression should have a right expression";
        return Translation.translateAsExpression(rightExpression, context);
    }

    public static boolean isIntrinsicOperation(@NotNull TranslationContext context,
                                               @NotNull JetOperationExpression expression) {
        FunctionDescriptor operationDescriptor =
                BindingUtils.getFunctionDescriptorForOperationExpression(context.bindingContext(), expression);

        if (operationDescriptor == null) return true;
        if (context.intrinsics().isIntrinsic(operationDescriptor)) return true;

        return false;
    }

    @NotNull
    public static JsNameRef getMethodReferenceForOverloadedOperation(@NotNull TranslationContext context,
                                                                     @NotNull JetOperationExpression expression) {
        FunctionDescriptor overloadedOperationDescriptor = getFunctionDescriptorForOperationExpression
                (context.bindingContext(), expression);
        assert overloadedOperationDescriptor != null;
        JsNameRef overloadedOperationReference = context.getNameForDescriptor(overloadedOperationDescriptor).makeRef();
        assert overloadedOperationReference != null;
        return overloadedOperationReference;
    }

    @NotNull
    public static JsFunction functionWithScope(@NotNull NamingScope scope) {
        return JsFunction.getAnonymousFunctionWithScope(scope.jsScope());
    }

    @NotNull
    public static JsNumberLiteral zeroLiteral(@NotNull TranslationContext context) {
        return context.program().getNumberLiteral(0);
    }

    @NotNull
    public static TemporaryVariable newAliasForThis(@NotNull TranslationContext context,
                                                    @NotNull DeclarationDescriptor descriptor) {
        JsExpression thisQualifier = getThisObject(context, descriptor);
        TemporaryVariable aliasForThis = context.declareTemporary(thisQualifier);
        context.aliaser().setAliasForThis(descriptor, aliasForThis.name());
        return aliasForThis;
    }

    public static void removeAliasForThis(@NotNull TranslationContext context,
                                          @NotNull DeclarationDescriptor descriptor) {
        context.aliaser().removeAliasForThis(descriptor);
    }

}
