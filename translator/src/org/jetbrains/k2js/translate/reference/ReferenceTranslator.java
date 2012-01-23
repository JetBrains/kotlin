package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;

/**
 * @author Pavel Talanov
 */
//TODO: get rid of the class, move to util
public final class ReferenceTranslator {

    @NotNull
    public static JsExpression translateSimpleName(@NotNull JetSimpleNameExpression expression,
                                                   @NotNull TranslationContext context) {
        if (PropertyAccessTranslator.canBePropertyAccess(expression, context)) {
            return PropertyAccessTranslator.translateAsPropertyGetterCall(expression, context);
        }
        DeclarationDescriptor referencedDescriptor =
                getDescriptorForReferenceExpression(context.bindingContext(), expression);
        return translateAsLocalNameReference(referencedDescriptor, context);
    }

    @NotNull
    public static JsExpression translateAsFQReference(@NotNull DeclarationDescriptor referencedDescriptor,
                                                      @NotNull TranslationContext context) {
        if (!context.hasQualifierForDescriptor(referencedDescriptor)) {
            return translateAsLocalNameReference(referencedDescriptor, context);
        }
        JsName referencedName = context.getNameForDescriptor(referencedDescriptor);
        JsExpression qualifier = context.getQualifierForDescriptor(referencedDescriptor);
        return AstUtil.qualified(referencedName, qualifier);
    }

    @NotNull
    public static JsExpression translateAsLocalNameReference(@NotNull DeclarationDescriptor referencedDescriptor,
                                                             @NotNull TranslationContext context) {

        if (referencedDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor) {
            return translateAsFQReference(referencedDescriptor, context);
        }
        JsName referencedName = context.getNameForDescriptor(referencedDescriptor);
        return referencedName.makeRef();
    }
}
