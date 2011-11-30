package org.jetbrains.k2js.translate.utils;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class TranslationUtils {

    @NotNull
    static public JsBinaryOperation notNullCheck(@NotNull TranslationContext context,
                                                 @NotNull JsExpression expressionToCheck) {
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        return AstUtil.notEqual(expressionToCheck, nullLiteral);
    }

    @NotNull
    static public JsBinaryOperation isNullCheck(@NotNull TranslationContext context,
                                                @NotNull JsExpression expressionToCheck) {
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        return AstUtil.equals(expressionToCheck, nullLiteral);
    }

    @Nullable
    static public JsName getLocalReferencedName(@NotNull TranslationContext context,
                                                @NotNull String name) {
        return context.enclosingScope().findExistingName(name);
    }


    @NotNull
    static public List<JsExpression> translateArgumentList(@NotNull List<? extends ValueArgument> jetArguments,
                                                           @NotNull TranslationContext context) {
        List<JsExpression> jsArguments = new ArrayList<JsExpression>();
        for (ValueArgument argument : jetArguments) {
            jsArguments.add(translateArgument(context, argument));
        }
        return jsArguments;
    }

    @NotNull
    static public JsExpression translateArgument(@NotNull TranslationContext context, @NotNull ValueArgument argument) {
        JetExpression jetExpression = argument.getArgumentExpression();
        if (jetExpression == null) {
            throw new AssertionError("Argument with no expression encountered!");
        }
        return Translation.translateAsExpression(jetExpression, context);
    }

    @NotNull
    static public JsNameRef backingFieldReference(@NotNull TranslationContext context,
                                                  @NotNull JetProperty expression) {
        JsName backingFieldName = getBackingFieldName(getPropertyName(expression), context);
        return getThisQualifiedNameReference(context, backingFieldName);
    }

    @NotNull
    static public JsNameRef backingFieldReference(@NotNull TranslationContext context,
                                                  @NotNull PropertyDescriptor descriptor) {
        JsName backingFieldName = getBackingFieldName(descriptor.getName(), context);
        return getThisQualifiedNameReference(context, backingFieldName);
    }

    @NotNull
    static public String getPropertyName(@NotNull JetProperty expression) {
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

    public static boolean isConstructorInvocation(@NotNull TranslationContext context,
                                                  @NotNull JetCallExpression expression) {
        JetExpression calleeExpression = expression.getCalleeExpression();
        assert calleeExpression != null : "JetCallExpression should have not null callee";
        ResolvedCall<?> resolvedCall = BindingUtils.getResolvedCall(context.bindingContext(), calleeExpression);
        if (resolvedCall == null) {
            return false;
        }
        CallableDescriptor descriptor = resolvedCall.getCandidateDescriptor();
        return (descriptor instanceof ConstructorDescriptor);
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
    private static JsExpression getThisQualifier(@NotNull TranslationContext context) {
        JsExpression qualifier;
        if (context.aliaser().hasAliasForThis()) {
            qualifier = context.aliaser().getAliasForThis();
        } else {
            qualifier = new JsThisRef();
        }
        return qualifier;
    }
}
