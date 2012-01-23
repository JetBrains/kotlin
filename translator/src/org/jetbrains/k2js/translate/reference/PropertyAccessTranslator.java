package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.Arrays;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getResolvedCall;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getSelectorAsSimpleName;
import static org.jetbrains.k2js.translate.utils.PsiUtils.isBackingFieldReference;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.backingFieldReference;

/**
 * @author Pavel Talanov
 */
public final class PropertyAccessTranslator extends AccessTranslator {

    private static final String MESSAGE = "Cannot be accessor call. Use canBeProperty*Call to ensure this method " +
            "can be called safely.";

    @NotNull
    private static PropertyDescriptor getPropertyDescriptor(@NotNull JetSimpleNameExpression expression,
                                                            @NotNull TranslationContext context) {
        DeclarationDescriptor descriptor =
                getDescriptorForReferenceExpression(context.bindingContext(), expression);
        assert descriptor instanceof PropertyDescriptor : "Must be a property descriptor.";
        return (PropertyDescriptor) descriptor;
    }

    @NotNull
    public static JsExpression translateAsPropertyGetterCall(@NotNull PropertyDescriptor descriptor,
                                                             @NotNull ResolvedCall<?> resolvedCall,
                                                             @NotNull TranslationContext context) {
        return (newInstance(descriptor, resolvedCall, context))
                .translateAsGet();
    }

    @NotNull
    public static JsExpression translateAsPropertyGetterCall(@NotNull JetSimpleNameExpression expression,
                                                             @NotNull TranslationContext context) {
        return (newInstance(expression, context))
                .translateAsGet();
    }

    @NotNull
    public static JsExpression translateAsPropertyGetterCall(@NotNull JetQualifiedExpression expression,
                                                             @NotNull TranslationContext context) {
        return (newInstance(expression, context))
                .translateAsGet();
    }

    @NotNull
    private static PropertyAccessTranslator newInstance(@NotNull PropertyDescriptor descriptor,
                                                        @NotNull ResolvedCall<?> resolvedCall,
                                                        @NotNull TranslationContext context) {
        return new PropertyAccessTranslator(descriptor, null, false, resolvedCall, context);
    }

    @NotNull
    public static PropertyAccessTranslator newInstance(@NotNull JetQualifiedExpression expression,
                                                       @NotNull TranslationContext context) {
        JetExpression qualifier = expression.getReceiverExpression();
        JetSimpleNameExpression selector = getNotNullSelector(expression);
        ResolvedCall<?> resolvedCall = getResolvedCall(context.bindingContext(), selector);
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(selector, context);
        boolean isBackingFieldAccess = isBackingFieldReference(selector);
        JsExpression jsQualifier = Translation.translateAsExpression(qualifier, context);
        return new PropertyAccessTranslator(propertyDescriptor, jsQualifier,
                isBackingFieldAccess, resolvedCall, context);
    }

    @NotNull
    public static PropertyAccessTranslator newInstance(@NotNull JetSimpleNameExpression expression,
                                                       @NotNull TranslationContext context) {
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(expression, context);
        ResolvedCall<?> resolvedCall = getResolvedCall(context.bindingContext(), expression);
        return new PropertyAccessTranslator(propertyDescriptor, null,
                isBackingFieldReference(expression), resolvedCall, context);
    }

    @NotNull
    public static PropertyAccessTranslator newInstance(@NotNull JetExpression expression,
                                                       @NotNull TranslationContext context) {
        if (expression instanceof JetQualifiedExpression) {
            return newInstance((JetQualifiedExpression) expression, context);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return newInstance((JetSimpleNameExpression) expression, context);
        }
        throw new AssertionError(MESSAGE);
    }

    public static boolean canBePropertyGetterCall(@NotNull JetQualifiedExpression expression,
                                                  @NotNull TranslationContext context) {
        JetSimpleNameExpression selector = getSelectorAsSimpleName(expression);
        if (selector == null) {
            return false;
        }
        return canBePropertyGetterCall(selector, context);
    }

    public static boolean canBePropertyGetterCall(@NotNull JetSimpleNameExpression expression,
                                                  @NotNull TranslationContext context) {
        return (getDescriptorForReferenceExpression
                (context.bindingContext(), expression) instanceof PropertyDescriptor);
    }

    public static boolean canBePropertyGetterCall(@NotNull JetExpression expression,
                                                  @NotNull TranslationContext context) {
        if (expression instanceof JetQualifiedExpression) {
            return canBePropertyGetterCall((JetQualifiedExpression) expression, context);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return canBePropertyGetterCall((JetSimpleNameExpression) expression, context);
        }
        return false;
    }

    public static boolean canBePropertyAccess(@NotNull JetExpression expression,
                                              @NotNull TranslationContext context) {
        return canBePropertyGetterCall(expression, context);
    }

    @Nullable
    private final JsExpression qualifier;
    @NotNull
    private final PropertyDescriptor propertyDescriptor;
    private final boolean isBackingFieldAccess;
    @NotNull
    ResolvedCall<?> resolvedCall;

    private PropertyAccessTranslator(@NotNull PropertyDescriptor descriptor,
                                     @Nullable JsExpression qualifier,
                                     boolean isBackingFieldAccess,
                                     @NotNull ResolvedCall<?> resolvedCall,
                                     @NotNull TranslationContext context) {
        super(context);
        this.qualifier = qualifier;
        this.propertyDescriptor = descriptor.getOriginal();
        this.isBackingFieldAccess = isBackingFieldAccess;
        this.resolvedCall = resolvedCall;
    }

    @Override
    @NotNull
    public JsExpression translateAsGet() {
        if (isBackingFieldAccess) {
            return backingFieldGet();
        } else {
            return getterCall();
        }
    }

    @NotNull
    private JsExpression backingFieldGet() {
        return backingFieldReference(context(), propertyDescriptor);
    }

    @NotNull
    private JsExpression getterCall() {
        return CallTranslator.translate(qualifier, resolvedCall, propertyDescriptor.getGetter(), context());
    }

    @Override
    @NotNull
    public JsExpression translateAsSet(@NotNull JsExpression toSetTo) {
        if (isBackingFieldAccess) {
            return backingFieldAssignment(toSetTo);
        } else {
            return setterCall(toSetTo);
        }
    }

    @NotNull
    private JsExpression setterCall(@NotNull JsExpression toSetTo) {
        return CallTranslator.translate(qualifier, Arrays.asList(toSetTo),
                resolvedCall, propertyDescriptor.getSetter(), context());
    }

    @NotNull
    private JsExpression backingFieldAssignment(@NotNull JsExpression toSetTo) {
        JsNameRef backingFieldReference = backingFieldReference(context(), propertyDescriptor);
        return AstUtil.newAssignment(backingFieldReference, toSetTo);
    }

    @NotNull
    private static JetSimpleNameExpression getNotNullSelector(@NotNull JetQualifiedExpression qualifiedExpression) {
        JetSimpleNameExpression selectorExpression = getSelectorAsSimpleName(qualifiedExpression);
        assert selectorExpression != null : MESSAGE;
        return selectorExpression;
    }
}
