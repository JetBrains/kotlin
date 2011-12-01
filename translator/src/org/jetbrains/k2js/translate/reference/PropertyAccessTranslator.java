package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyGetterDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertySetterDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.PsiUtils;

/**
 * @author Talanov Pavel
 */
public final class PropertyAccessTranslator extends AbstractTranslator {

    private static String MESSAGE = "Cannot be accessor call. Use canBeProperty*Call to ensure this method " +
            "can be called safely.";

    @NotNull
    public static JsInvocation translateAsPropertySetterCall(@NotNull JetQualifiedExpression expression,
                                                             @NotNull TranslationContext context) {
        return (new PropertyAccessTranslator(expression, context))
                .translateAsPropertySetterCall();
    }

    @NotNull
    public static JsInvocation translateAsPropertySetterCall(@NotNull JetSimpleNameExpression expression,
                                                             @NotNull TranslationContext context) {
        return (new PropertyAccessTranslator(expression, context))
                .translateAsPropertySetterCall();
    }

    @NotNull
    public static JsInvocation translateAsPropertySetterCall(@NotNull JetExpression expression,
                                                             @NotNull TranslationContext context) {
        if (expression instanceof JetQualifiedExpression) {
            return translateAsPropertySetterCall((JetQualifiedExpression) expression, context);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return translateAsPropertySetterCall((JetSimpleNameExpression) expression, context);
        }
        throw new AssertionError(MESSAGE);
    }

    @NotNull
    public static JsInvocation translateAsPropertyGetterCall(@NotNull JetQualifiedExpression expression,
                                                             @NotNull TranslationContext context) {
        return (new PropertyAccessTranslator(expression, context))
                .translateAsPropertyGetterCall();
    }

    @NotNull
    public static JsInvocation translateAsPropertyGetterCall(@NotNull JetSimpleNameExpression expression,
                                                             @NotNull TranslationContext context) {
        return (new PropertyAccessTranslator(expression, context))
                .translateAsPropertyGetterCall();
    }

    @NotNull
    public static JsInvocation translateAsPropertyGetterCall(@NotNull JetExpression expression,
                                                             @NotNull TranslationContext context) {
        if (expression instanceof JetQualifiedExpression) {
            return translateAsPropertyGetterCall((JetQualifiedExpression) expression, context);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return translateAsPropertyGetterCall((JetSimpleNameExpression) expression, context);
        }
        throw new AssertionError(MESSAGE);
    }

    public static boolean canBePropertyGetterCall(@NotNull JetQualifiedExpression expression,
                                                  @NotNull TranslationContext context) {
        JetSimpleNameExpression selector = PsiUtils.getSelectorAsSimpleName(expression);
        if (selector == null) {
            return false;
        }
        return canBePropertyGetterCall(selector, context);
    }

    public static boolean canBePropertyGetterCall(@NotNull JetSimpleNameExpression expression,
                                                  @NotNull TranslationContext context) {
        return (BindingUtils.getPropertyDescriptorForSimpleName(context.bindingContext(), expression) != null);
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


    @NotNull
    private final JetSimpleNameExpression expression;
    @Nullable
    private final JetExpression qualifier;
    @NotNull
    private final PropertyDescriptor propertyDescriptor;

    private PropertyAccessTranslator(@NotNull JetSimpleNameExpression simpleName,
                                     @NotNull TranslationContext context) {
        super(context);
        this.expression = simpleName;
        this.qualifier = null;
        this.propertyDescriptor = getNotNullPropertyDescriptor();
    }

    private PropertyAccessTranslator(@NotNull JetQualifiedExpression qualifiedExpression,
                                     @NotNull TranslationContext context) {
        super(context);
        this.expression = getNotNullSelector(qualifiedExpression);
        this.qualifier = qualifiedExpression.getReceiverExpression();
        this.propertyDescriptor = getNotNullPropertyDescriptor();
    }

    @NotNull
    public JsInvocation translateAsPropertyGetterCall() {
        JsName getterName = getNotNullGetterName();
        return qualifiedAccessorInvocation(getterName);
    }

    @NotNull
    private JsInvocation qualifiedAccessorInvocation(@NotNull JsName accessorName) {
        JsNameRef accessorReference = accessorName.makeRef();
        AstUtil.setQualifier(accessorReference, translateQualifier());
        return AstUtil.newInvocation(accessorReference);
    }

    @NotNull
    public JsInvocation translateAsPropertySetterCall() {
        JsName setterName = getNotNullSetterName();
        return qualifiedAccessorInvocation(setterName);
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
        JetSimpleNameExpression selectorExpression = PsiUtils.getSelectorAsSimpleName(qualifiedExpression);
        assert selectorExpression != null : MESSAGE;
        return selectorExpression;
    }

    @NotNull
    private JsName getNotNullGetterName() {
        JsName getterName = getNullableGetterName();
        assert getterName != null : MESSAGE;
        return getterName;
    }

    @Nullable
    private JsName getNullableGetterName() {
        PropertyGetterDescriptor getter = getGetterDescriptor();

        if (getter == null) return null;

        return context().getNameForDescriptor(getter);
    }

    @Nullable
    private PropertyGetterDescriptor getGetterDescriptor() {
        PropertyDescriptor property = getNullablePropertyDescriptor();
        if (property == null) {
            return null;
        }
        PropertyGetterDescriptor getter = property.getGetter();
        if (getter == null) {
            return null;
        }
        return getter;
    }

    @NotNull
    private JsName getNotNullSetterName() {
        JsName setterName = getNullableSetterName();
        assert setterName != null : MESSAGE;
        return setterName;
    }

    @Nullable
    private JsName getNullableSetterName() {
        PropertySetterDescriptor setter = getSetterDescriptor();

        if (setter == null) return null;

        return context().getNameForDescriptor(setter);
    }

    @Nullable
    private PropertySetterDescriptor getSetterDescriptor() {
        PropertyDescriptor property = getNullablePropertyDescriptor();
        if (property == null) {
            return null;
        }
        PropertySetterDescriptor setter = property.getSetter();
        if (setter == null) {
            return null;
        }
        return setter;
    }

    @Nullable
    private PropertyDescriptor getNullablePropertyDescriptor() {
        return BindingUtils.getPropertyDescriptorForSimpleName(context().bindingContext(), expression);
    }

    @NotNull
    private PropertyDescriptor getNotNullPropertyDescriptor() {
        PropertyDescriptor propertyDescriptor = getNullablePropertyDescriptor();
        assert propertyDescriptor != null;
        return propertyDescriptor;
    }
}
