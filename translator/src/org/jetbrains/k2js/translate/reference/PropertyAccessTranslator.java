package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyGetterDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertySetterDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getSelectorAsSimpleName;
import static org.jetbrains.k2js.translate.utils.PsiUtils.isBackingFieldReference;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.backingFieldReference;

/**
 * @author Talanov Pavel
 */
public final class PropertyAccessTranslator extends AccessTranslator {

    private static String MESSAGE = "Cannot be accessor call. Use canBeProperty*Call to ensure this method " +
            "can be called safely.";


    @NotNull
    public static JsExpression translateAsPropertyGetterCall(@NotNull JetQualifiedExpression expression,
                                                             @NotNull TranslationContext context) {
        return (new PropertyAccessTranslator(expression, context))
                .translateAsGet();
    }

    @NotNull
    public static JsExpression translateAsPropertyGetterCall(@NotNull PropertyDescriptor descriptor,
                                                             @NotNull TranslationContext context) {
        return (new PropertyAccessTranslator(descriptor, context))
                .translateAsGet();
    }


    @NotNull
    public static PropertyAccessTranslator newInstance(@NotNull JetQualifiedExpression expression,
                                                       @NotNull TranslationContext context) {
        return (new PropertyAccessTranslator(expression, context));
    }

    @NotNull
    public static PropertyAccessTranslator newInstance(@NotNull JetSimpleNameExpression expression,
                                                       @NotNull TranslationContext context) {
        return (new PropertyAccessTranslator(expression, context));
    }

    @NotNull
    public static JsExpression translateAsPropertyGetterCall(@NotNull JetSimpleNameExpression expression,
                                                             @NotNull TranslationContext context) {
        return (new PropertyAccessTranslator(expression, context))
                .translateAsGet();
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
    private final JetExpression qualifier;
    @NotNull
    private final PropertyDescriptor propertyDescriptor;
    private final boolean isBackingFieldAccess;

    private PropertyAccessTranslator(@NotNull JetSimpleNameExpression simpleName,
                                     @NotNull TranslationContext context) {
        super(context);
        this.qualifier = null;
        this.propertyDescriptor = getPropertyDescriptor(simpleName);
        this.isBackingFieldAccess = isBackingFieldReference(simpleName);
    }

    private PropertyAccessTranslator(@NotNull JetQualifiedExpression qualifiedExpression,
                                     @NotNull TranslationContext context) {
        super(context);
        this.qualifier = qualifiedExpression.getReceiverExpression();
        JetSimpleNameExpression selector = getNotNullSelector(qualifiedExpression);
        this.propertyDescriptor = getPropertyDescriptor(selector);
        this.isBackingFieldAccess = isBackingFieldReference(selector);
    }

    private PropertyAccessTranslator(@NotNull PropertyDescriptor descriptor,
                                     @NotNull TranslationContext context) {
        super(context);
        this.qualifier = null;
        this.propertyDescriptor = descriptor;
        this.isBackingFieldAccess = false;
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
        JsName getterName = getGetterName();
        return qualifiedAccessorInvocation(getterName);
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
        JsName setterName = getSetterName();
        JsInvocation setterCall = qualifiedAccessorInvocation(setterName);
        setterCall.getArguments().add(toSetTo);
        return setterCall;
    }

    @NotNull
    private JsExpression backingFieldAssignment(@NotNull JsExpression toSetTo) {
        JsNameRef backingFieldReference = backingFieldReference(context(), propertyDescriptor);
        return AstUtil.newAssignment(backingFieldReference, toSetTo);
    }

    @NotNull
    private JsInvocation qualifiedAccessorInvocation(@NotNull JsName accessorName) {
        JsNameRef accessorReference = accessorName.makeRef();
        AstUtil.setQualifier(accessorReference, translateQualifier());
        return AstUtil.newInvocation(accessorReference);
    }

    @NotNull
    private JsExpression translateQualifier() {
        if (qualifier != null) {
            return Translation.translateAsExpression(qualifier, context());
        }
        JsExpression implicitReceiver = ReferenceTranslator.getImplicitReceiver(propertyDescriptor, context());
        assert implicitReceiver != null : "Property can only be a member of class or a namespace.";
        return implicitReceiver;
    }


    @NotNull
    private static JetSimpleNameExpression getNotNullSelector(@NotNull JetQualifiedExpression qualifiedExpression) {
        JetSimpleNameExpression selectorExpression = getSelectorAsSimpleName(qualifiedExpression);
        assert selectorExpression != null : MESSAGE;
        return selectorExpression;
    }

    @NotNull
    private JsName getGetterName() {
        PropertyGetterDescriptor getter = getGetterDescriptor();
        return context().getNameForDescriptor(getter);
    }

    @NotNull
    private PropertyGetterDescriptor getGetterDescriptor() {
        PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
        assert getter != null;
        return getter;
    }

    @NotNull
    private JsName getSetterName() {
        PropertySetterDescriptor setter = getSetterDescriptor();
        return context().getNameForDescriptor(setter);
    }

    @NotNull
    private PropertySetterDescriptor getSetterDescriptor() {
        PropertySetterDescriptor setter = propertyDescriptor.getSetter();
        assert setter != null;
        return setter;
    }

    @NotNull
    private PropertyDescriptor getPropertyDescriptor(@NotNull JetSimpleNameExpression expression) {
        DeclarationDescriptor descriptor =
                getDescriptorForReferenceExpression(context().bindingContext(), expression);
        //TODO
        assert descriptor instanceof PropertyDescriptor;
        return (PropertyDescriptor) descriptor;
    }
}
