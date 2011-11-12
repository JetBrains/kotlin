package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsNode;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyGetterDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertySetterDescriptor;
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;

/**
 * @author Talanov Pavel
 */
//TODO ask for code review. Class has messy code.
public final class PropertyAccessTranslator extends AbstractTranslator {

    public PropertyAccessTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @Nullable
    public JsInvocation resolveAsPropertyGet(@NotNull JetDotQualifiedExpression expression) {
        JetExpression selectorExpression = expression.getSelectorExpression();
        assert selectorExpression != null : "Selector should not be null.";
        JsName getterName = getPropertyGetterName(selectorExpression);
        if (getterName == null) {
            return null;
        }
        return translateReceiverAndReturnAccessorInvocation(expression, getterName);
    }

    @Nullable
    JsInvocation resolveAsPropertyGet(@NotNull JetSimpleNameExpression expression) {
        JsName getterName = getPropertyGetterName(expression);
        if (getterName == null) {
            return null;
        }
        return AstUtil.newInvocation(AstUtil.thisQualifiedReference(getterName));
    }

    @NotNull
    private JsInvocation translateReceiverAndReturnAccessorInvocation
            (@NotNull JetDotQualifiedExpression dotQualifiedExpression, @NotNull JsName accessorName) {
        ExpressionTranslator translator = new ExpressionTranslator(translationContext());
        JsNode node = translator.translate(dotQualifiedExpression.getReceiverExpression());
        JsNameRef result = accessorName.makeRef();
        result.setQualifier(AstUtil.convertToExpression(node));
        return AstUtil.newInvocation(result);
    }

    @Nullable
    public JsInvocation resolveAsPropertySet(@NotNull JetExpression expression) {
        if (expression instanceof JetDotQualifiedExpression) {
            return resolveAsPropertySet((JetDotQualifiedExpression) expression);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return resolveAsPropertySet((JetSimpleNameExpression) expression);
        }
        return null;
    }

    @Nullable
    private JsInvocation resolveAsPropertySet(@NotNull JetDotQualifiedExpression dotQualifiedExpression) {
        JetExpression selectorExpression = dotQualifiedExpression.getSelectorExpression();
        assert selectorExpression != null : "Selector should not be null.";
        JsName setterName = getPropertySetterName(selectorExpression);
        if (setterName == null) {
            return null;
        }
        return translateReceiverAndReturnAccessorInvocation(dotQualifiedExpression, setterName);
    }

    @Nullable
    JsInvocation resolveAsPropertySet(@NotNull JetSimpleNameExpression expression) {
        JsName setterName = getPropertySetterName(expression);
        if (setterName == null) {
            return null;
        }
        return AstUtil.newInvocation(AstUtil.thisQualifiedReference(setterName));
    }

    @Nullable
    public JsName getPropertyGetterName(@NotNull JetExpression expression) {
        PropertyDescriptor property = getPropertyDescriptor(expression);
        if (property == null) {
            return null;
        }
        PropertyGetterDescriptor getter = property.getGetter();
        if (getter == null) {
            return null;
        }
        return translationContext().getNameForDescriptor(getter);
    }

    @Nullable
    public JsName getPropertySetterName(@NotNull JetExpression expression) {
        PropertyDescriptor property = getPropertyDescriptor(expression);
        if (property == null) {
            return null;
        }
        PropertySetterDescriptor setter = property.getSetter();
        if (setter == null) {
            return null;
        }
        return translationContext().getNameForDescriptor(setter);
    }

    @Nullable
    private PropertyDescriptor getPropertyDescriptor(@NotNull JetExpression expression) {
        ResolvedCall<?> resolvedCall =
                translationContext().bindingContext().get(BindingContext.RESOLVED_CALL, expression);
        if (resolvedCall != null) {
            DeclarationDescriptor descriptor = resolvedCall.getCandidateDescriptor();
            if (descriptor instanceof PropertyDescriptor) {
                return (PropertyDescriptor) descriptor;
            }
        }
        return null;
    }

}
