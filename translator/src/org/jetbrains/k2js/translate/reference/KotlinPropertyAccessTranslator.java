package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.Arrays;

import static org.jetbrains.k2js.translate.utils.TranslationUtils.backingFieldReference;

/**
 * @author Pavel Talanov
 */
public final class KotlinPropertyAccessTranslator extends PropertyAccessTranslator {

    @Nullable
    private final JsExpression qualifier;
    @NotNull
    private final PropertyDescriptor propertyDescriptor;
    private final boolean isBackingFieldAccess;
    @NotNull
    ResolvedCall<?> resolvedCall;

    //TODO: too many params in constructor
    /*package*/ KotlinPropertyAccessTranslator(@NotNull PropertyDescriptor descriptor,
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
}
