package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.Arrays;

/**
 * @author Pavel Talanov
 *         <p/>
 *         For properies /w accessors.
 */
public final class KotlinPropertyAccessTranslator extends PropertyAccessTranslator {

    @Nullable
    private final JsExpression qualifier;
    @NotNull
    private final PropertyDescriptor propertyDescriptor;
    @NotNull
    ResolvedCall<?> resolvedCall;

    //TODO: too many params in constructor
    /*package*/ KotlinPropertyAccessTranslator(@NotNull PropertyDescriptor descriptor,
                                               @Nullable JsExpression qualifier,
                                               @NotNull ResolvedCall<?> resolvedCall,
                                               @NotNull TranslationContext context) {
        super(context);
        this.qualifier = qualifier;
        this.propertyDescriptor = descriptor.getOriginal();
        this.resolvedCall = resolvedCall;
    }

    @Override
    @NotNull
    public JsExpression translateAsGet() {
        return CallTranslator.translate(qualifier, resolvedCall, propertyDescriptor.getGetter(), getCallType(), context());
    }

    @Override
    @NotNull
    public JsExpression translateAsSet(@NotNull JsExpression toSetTo) {

        return CallTranslator.translate(qualifier, Arrays.asList(toSetTo),
                resolvedCall, propertyDescriptor.getSetter(), getCallType(), context());
    }

}
