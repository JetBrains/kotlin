package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;

/**
 * @author Pavel Talanov
 */
public final class ReferenceTranslator {

    @NotNull
    public static JsExpression translateSimpleName(@NotNull JetSimpleNameExpression expression,
                                                   @NotNull TranslationContext context) {
        if (PropertyAccessTranslator.canBePropertyGetterCall(expression, context)) {
            return PropertyAccessTranslator.translateAsPropertyGetterCall(expression, null, context);
        }
        DeclarationDescriptor referencedDescriptor =
                getDescriptorForReferenceExpression(context.bindingContext(), expression);
        return translateAsLocalNameReference(referencedDescriptor, context);
    }

    @NotNull
    public static JsExpression translateAsFQReference(@NotNull DeclarationDescriptor referencedDescriptor,
                                                      @NotNull TranslationContext context) {
        JsExpression qualifier = context.getQualifierForDescriptor(referencedDescriptor);
        if (qualifier == null) {
            return translateAsLocalNameReference(referencedDescriptor, context);
        }
        JsName referencedName = context.getNameForDescriptor(referencedDescriptor);
        return AstUtil.qualified(referencedName, qualifier);
    }

    @NotNull
    public static JsExpression translateAsLocalNameReference(@NotNull DeclarationDescriptor referencedDescriptor,
                                                             @NotNull TranslationContext context) {
        JsName referencedName = context.getNameForDescriptor(referencedDescriptor);
        return referencedName.makeRef();
    }
}
