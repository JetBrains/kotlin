package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static org.jetbrains.k2js.translate.utils.AnnotationsUtils.isNativeObject;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getResolvedCall;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getSelectorAsSimpleName;
import static org.jetbrains.k2js.translate.utils.PsiUtils.isBackingFieldReference;

/**
 * @author Pavel Talanov
 */
public abstract class PropertyAccessTranslator extends AccessTranslator {

    @NotNull
    public static PropertyAccessTranslator newInstance(@NotNull PropertyDescriptor descriptor,
                                                       @NotNull ResolvedCall<?> resolvedCall,
                                                       @NotNull TranslationContext context) {
        if (isNativeObject(descriptor)) {
            return new NativePropertyAccessTranslator(descriptor, /*qualifier = */ null, context);
        } else {
            return new KotlinPropertyAccessTranslator(descriptor, /*qualifier = */ null, /*backingFieldAccess = */ false,
                    resolvedCall, context);
        }
    }

    @NotNull
    public static PropertyAccessTranslator newInstance(@NotNull JetSimpleNameExpression expression,
                                                       @Nullable JsExpression qualifier,
                                                       @NotNull TranslationContext context) {
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(expression, context);
        if (isNativeObject(propertyDescriptor)) {
            return new NativePropertyAccessTranslator(propertyDescriptor, qualifier, context);
        }
        ResolvedCall<?> resolvedCall = getResolvedCall(context.bindingContext(), expression);
        boolean backingFieldAccess = isBackingFieldReference(expression);
        return new KotlinPropertyAccessTranslator(propertyDescriptor, qualifier,
                backingFieldAccess, resolvedCall, context);
    }

    @NotNull
    /*package*/ static PropertyDescriptor getPropertyDescriptor(@NotNull JetSimpleNameExpression expression,
                                                                @NotNull TranslationContext context) {
        DeclarationDescriptor descriptor =
                getDescriptorForReferenceExpression(context.bindingContext(), expression);
        assert descriptor instanceof PropertyDescriptor : "Must be a property descriptor.";
        return (PropertyDescriptor) descriptor;
    }


    @NotNull
    /*package*/
    static JsExpression translateAsPropertyGetterCall(@NotNull PropertyDescriptor descriptor,
                                                      @NotNull ResolvedCall<?> resolvedCall,
                                                      @NotNull TranslationContext context) {
        return (newInstance(descriptor, resolvedCall, context))
                .translateAsGet();
    }

    @NotNull
    public static JsExpression translateAsPropertyGetterCall(@NotNull JetSimpleNameExpression expression,
                                                             @Nullable JsExpression qualifier,
                                                             @NotNull TranslationContext context) {
        return (newInstance(expression, qualifier, context))
                .translateAsGet();
    }


    public static boolean canBePropertyGetterCall(@NotNull JetQualifiedExpression expression,
                                                  @NotNull TranslationContext context) {
        JetSimpleNameExpression selector = getSelectorAsSimpleName(expression);
        assert selector != null : "Only names are allowed after the dot";
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

    protected PropertyAccessTranslator(@NotNull TranslationContext context) {
        super(context);
    }

}