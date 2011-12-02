package org.jetbrains.k2js.translate.utils;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptorForOperationExpression;

/**
 * @author Talanov Pavel
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

    @Nullable
    public static JsName getLocalReferencedName(@NotNull TranslationContext context,
                                                @NotNull String name) {
        return context.enclosingScope().findExistingName(name);
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
    public static JsExpression translateArgument(@NotNull TranslationContext context, @NotNull ValueArgument argument) {
        JetExpression jetExpression = argument.getArgumentExpression();
        if (jetExpression == null) {
            throw new AssertionError("Argument with no expression encountered!");
        }
        return Translation.translateAsExpression(jetExpression, context);
    }

    @NotNull
    public static JsNameRef backingFieldReference(@NotNull TranslationContext context,
                                                  @NotNull JetProperty expression) {
        JsName backingFieldName = getBackingFieldName(getPropertyName(expression), context);
        return getThisQualifiedNameReference(context, backingFieldName);
    }

    @NotNull
    public static JsNameRef backingFieldReference(@NotNull TranslationContext context,
                                                  @NotNull PropertyDescriptor descriptor) {
        JsName backingFieldName = getBackingFieldName(descriptor.getName(), context);
        if (BindingUtils.isOwnedByClass(descriptor)) {
            return getThisQualifiedNameReference(context, backingFieldName);
        }
        assert BindingUtils.isOwnedByNamespace(descriptor)
                : "Only classes and namespaces may own descriptors";
        JsNameRef qualifier = context.declarations().getQualifier(descriptor);
        return AstUtil.qualified(backingFieldName, qualifier);
    }

    @NotNull
    public static String getPropertyName(@NotNull JetProperty expression) {
        String propertyName = expression.getName();
        if (propertyName == null) {
            throw new AssertionError("Property with no name encountered!");
        }
        return propertyName;
    }

    @NotNull
    static private JsName getBackingFieldName(@NotNull String propertyName,
                                              @NotNull TranslationContext context) {
        String backingFieldName = Namer.getKotlinBackingFieldName(propertyName);
        return context.enclosingScope().findExistingName(backingFieldName);
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


    public static boolean hasQualifier(@NotNull TranslationContext context, @NotNull DeclarationDescriptor descriptor) {
        return context.declarations().hasQualifier(descriptor);
    }

    @NotNull
    public static JsNameRef getQualifiedReference(@NotNull TranslationContext context,
                                                  @NotNull DeclarationDescriptor descriptor) {
        JsName name = context.declarations().getName(descriptor);
        JsNameRef reference = name.makeRef();
        if (hasQualifier(context, descriptor)) {
            JsNameRef qualifier = getQualifier(context, descriptor);
            AstUtil.setQualifier(reference, qualifier);
        }
        return reference;
    }

    @NotNull
    private static JsNameRef getQualifier(@NotNull TranslationContext context,
                                          @NotNull DeclarationDescriptor descriptor) {
        return context.declarations().getQualifier(descriptor);
    }

    @NotNull
    public static JsNameRef getThisQualifiedNameReference(@NotNull TranslationContext context,
                                                          @NotNull JsName name) {
        JsExpression qualifier = getThisQualifier(context);
        JsNameRef reference = name.makeRef();
        AstUtil.setQualifier(reference, qualifier);
        return reference;
    }

    @NotNull
    public static JsExpression getThisQualifier(@NotNull TranslationContext context) {
        JsExpression qualifier;
        if (context.aliaser().hasAliasForThis()) {
            qualifier = context.aliaser().getAliasForThis();
        } else {
            qualifier = new JsThisRef();
        }
        return qualifier;
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

}
