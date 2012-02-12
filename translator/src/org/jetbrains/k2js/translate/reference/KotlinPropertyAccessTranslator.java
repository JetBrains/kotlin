package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyGetterDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertySetterDescriptor;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.k2js.translate.context.TranslationContext;

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
        //TODO: check for duplication
        PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
        assert getter != null : "Getter for kotlin properties should bot be null.";
        return callBuilderForAccessor()
                .descriptor(getter)
                .translate();
    }

    @Override
    @NotNull
    public JsExpression translateAsSet(@NotNull JsExpression toSetTo) {
        //TODO: check for duplication
        PropertySetterDescriptor setter = propertyDescriptor.getSetter();
        assert setter != null : "Getter for kotlin properties should bot be null.";
        return callBuilderForAccessor()
                .args(toSetTo)
                .descriptor(setter)
                .translate();
    }

    @NotNull
    private CallBuilder callBuilderForAccessor() {
        return CallBuilder.build(context())
                .receiver(qualifier)
                .resolvedCall(resolvedCall)
                .type(getCallType());
    }

}
