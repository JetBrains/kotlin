package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getExpectedThisDescriptor;

/**
 * @author Pavel Talanov
 */
public final class NativePropertyAccessTranslator extends PropertyAccessTranslator {


    @Nullable
    private final JsExpression qualifier;
    @NotNull
    private final PropertyDescriptor propertyDescriptor;

    /*package*/
    NativePropertyAccessTranslator(@NotNull PropertyDescriptor descriptor,
                                   @Nullable JsExpression qualifier,
                                   @NotNull TranslationContext context) {
        super(context);
        this.qualifier = qualifier;
        this.propertyDescriptor = descriptor.getOriginal();
    }


    @Override
    @NotNull
    public JsExpression translateAsGet() {
        JsName nativePropertyName = context().getNameForDescriptor(propertyDescriptor);
        JsExpression realQualifier = getQualifier();
        if (realQualifier != null) {
            return AstUtil.qualified(nativePropertyName, realQualifier);
        } else {
            return nativePropertyName.makeRef();
        }
    }

    @Override
    @NotNull
    public JsExpression translateAsSet(@NotNull JsExpression setTo) {
        return AstUtil.assignment(translateAsGet(), setTo);
    }

    @Nullable
    public JsExpression getQualifier() {
        if (qualifier != null) {
            return qualifier;
        }
        assert !propertyDescriptor.getReceiverParameter().exists() : "Cant have native extension properties.";
        DeclarationDescriptor expectedThisDescriptor = getExpectedThisDescriptor(propertyDescriptor);
        if (expectedThisDescriptor == null) {
            return null;
        }
        return TranslationUtils.getThisObject(context(), expectedThisDescriptor);
    }
}
