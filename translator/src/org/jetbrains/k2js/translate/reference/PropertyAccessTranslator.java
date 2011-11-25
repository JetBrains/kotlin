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
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.utils.BindingUtils;

/**
 * @author Talanov Pavel
 */
//TODO ask for code review. Class has messy code.
public final class PropertyAccessTranslator extends AbstractTranslator {

    private static String MESSAGE = "Cannot be accessor call. Use canBeProperty*Call to ensure this method " +
            "can be called safely.";

    @NotNull
    public static PropertyAccessTranslator newInstance(@NotNull TranslationContext context) {
        return new PropertyAccessTranslator(context);
    }

    private PropertyAccessTranslator(@NotNull TranslationContext context) {
        super(context);
    }


    //TODO: ask if these methods can be more clear
    public boolean canBePropertyGetterCall(@NotNull JetExpression expression) {
        if (expression instanceof JetQualifiedExpression) {
            JetSimpleNameExpression selector = getNullableSelector((JetQualifiedExpression) expression);
            if (selector == null) {
                return false;
            }
            return (getNullableGetterName(selector) != null);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return (getNullableGetterName((JetSimpleNameExpression) expression) != null);
        }
        return false;
    }

    public boolean canBePropertyAccess(@NotNull JetExpression expression) {
        return canBePropertyGetterCall(expression);
    }

    public boolean canBePropertySetterCall(@NotNull JetExpression expression) {
        if (expression instanceof JetQualifiedExpression) {
            JetSimpleNameExpression selector = getNullableSelector((JetQualifiedExpression) expression);
            if (selector == null) {
                return false;
            }
            return (getNullableSetterName(selector) != null);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return (getNullableSetterName((JetSimpleNameExpression) expression) != null);
        }
        return false;
    }

    @NotNull
    public JsInvocation translateAsPropertyGetterCall(@NotNull JetExpression expression) {
        if (expression instanceof JetQualifiedExpression) {
            return resolveAsPropertyGet((JetQualifiedExpression) expression);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return resolveAsPropertyGet((JetSimpleNameExpression) expression);
        }
        throw new AssertionError(MESSAGE);
    }

    @NotNull
    private JsInvocation resolveAsPropertyGet(@NotNull JetQualifiedExpression expression) {
        JsName getterName = getNotNullGetterName(getNotNullSelector(expression));
        return translateReceiverAndReturnAccessorInvocation(expression, getterName);
    }

    @NotNull
    private JsInvocation resolveAsPropertyGet(@NotNull JetSimpleNameExpression expression) {
        JsName getterName = getNotNullGetterName(expression);
        JsNameRef getterReference = ReferenceProvider.getReference(getterName, context(), expression);
        return AstUtil.newInvocation(getterReference);
    }

    @NotNull
    public JsInvocation translateAsPropertySetterCall(@NotNull JetExpression expression) {
        if (expression instanceof JetDotQualifiedExpression) {
            return resolveAsPropertySet((JetDotQualifiedExpression) expression);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return resolveAsPropertySet((JetSimpleNameExpression) expression);
        }
        throw new AssertionError(MESSAGE);
    }

    @NotNull
    private JsInvocation resolveAsPropertySet(@NotNull JetDotQualifiedExpression dotQualifiedExpression) {
        JsName setterName = getNotNullSetterName(getNotNullSelector(dotQualifiedExpression));
        return translateReceiverAndReturnAccessorInvocation(dotQualifiedExpression, setterName);
    }

    @NotNull
    private JsInvocation resolveAsPropertySet(@NotNull JetSimpleNameExpression expression) {
        JsName setterName = getNotNullSetterName(expression);
        JsNameRef setterReference = ReferenceProvider.getReference(setterName, context(), expression);
        return AstUtil.newInvocation(setterReference);
    }

    @NotNull
    private JsInvocation translateReceiverAndReturnAccessorInvocation
            (@NotNull JetQualifiedExpression qualifiedExpression, @NotNull JsName accessorName) {
        JsExpression qualifier = Translation.translateAsExpression
                (qualifiedExpression.getReceiverExpression(), context());
        JsNameRef result = accessorName.makeRef();
        AstUtil.setQualifier(result, qualifier);
        return AstUtil.newInvocation(result);
    }

    @Nullable
    private JetSimpleNameExpression getNullableSelector(@NotNull JetQualifiedExpression expression) {
        JetExpression selectorExpression = expression.getSelectorExpression();
        assert selectorExpression != null : "Selector should not be null.";
        if (!(selectorExpression instanceof JetSimpleNameExpression)) {
            return null;
        }
        return (JetSimpleNameExpression) selectorExpression;
    }


    @NotNull
    private JetSimpleNameExpression getNotNullSelector(@NotNull JetQualifiedExpression expression) {
        JetSimpleNameExpression selectorExpression = getNullableSelector(expression);
        assert selectorExpression != null : MESSAGE;
        return selectorExpression;
    }

    @NotNull
    private JsName getNotNullGetterName(@NotNull JetSimpleNameExpression selectorExpression) {
        JsName getterName = getNullableGetterName(selectorExpression);
        assert getterName != null : MESSAGE;
        return getterName;
    }

    @Nullable
    private JsName getNullableGetterName(@NotNull JetSimpleNameExpression expression) {
        PropertyDescriptor property = getPropertyDescriptor(expression);
        if (property == null) {
            return null;
        }
        PropertyGetterDescriptor getter = property.getGetter();
        if (getter == null) {
            return null;
        }
        return context().getNameForDescriptor(getter);
    }


    @NotNull
    private JsName getNotNullSetterName(@NotNull JetSimpleNameExpression selectorExpression) {
        JsName setterName = getNullableSetterName(selectorExpression);
        assert setterName != null : MESSAGE;
        return setterName;
    }

    @Nullable
    private JsName getNullableSetterName(@NotNull JetSimpleNameExpression expression) {
        PropertyDescriptor property = getPropertyDescriptor(expression);
        if (property == null) {
            return null;
        }
        PropertySetterDescriptor setter = property.getSetter();
        if (setter == null) {
            return null;
        }
        return context().getNameForDescriptor(setter);
    }

    @Nullable
    private PropertyDescriptor getPropertyDescriptor(@NotNull JetSimpleNameExpression expression) {
        ResolvedCall<?> resolvedCall =
                BindingUtils.getResolvedCall(context().bindingContext(), expression);
        if (resolvedCall != null) {
            DeclarationDescriptor descriptor = resolvedCall.getCandidateDescriptor();
            if (descriptor instanceof PropertyDescriptor) {
                return (PropertyDescriptor) descriptor;
            }
        }
        return null;
    }

}
