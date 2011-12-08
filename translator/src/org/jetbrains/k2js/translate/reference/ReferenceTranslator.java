package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.getImplicitReceiver;

/**
 * @author Talanov Pavel
 */
public class ReferenceTranslator extends AbstractTranslator {

    @NotNull
    public static JsExpression translateSimpleName(@NotNull JetSimpleNameExpression expression,
                                                   @NotNull TranslationContext context) {
        if (PropertyAccessTranslator.canBePropertyAccess(expression, context)) {
            return PropertyAccessTranslator.translateAsPropertyGetterCall(expression, context);
        }
        DeclarationDescriptor referencedDescriptor =
                getDescriptorForReferenceExpression(context.bindingContext(), expression);
        return (new ReferenceTranslator(referencedDescriptor, true, context)).translate();
    }

    @NotNull
    public static JsExpression translateReference(@NotNull DeclarationDescriptor referencedDescriptor,
                                                  @NotNull TranslationContext context) {
        return (new ReferenceTranslator(referencedDescriptor, false, context)).translate();
    }

    @NotNull
    private final DeclarationDescriptor referencedDescriptor;

    private final boolean shouldQualify;

    private ReferenceTranslator(@NotNull DeclarationDescriptor referencedDescriptor,
                                boolean shouldQualify,
                                @NotNull TranslationContext context) {
        super(context);
        this.referencedDescriptor = referencedDescriptor;
        this.shouldQualify = shouldQualify;
    }

    @NotNull
    public JsExpression translate() {
        JsName referencedName = context().getNameForDescriptor(referencedDescriptor);
        JsExpression implicitReceiver = getImplicitReceiver(context(), referencedDescriptor);

        return generateReference(referencedName, implicitReceiver);
    }

    @NotNull
    private JsExpression generateReference(@NotNull JsName referencedName, @Nullable JsExpression implicitReceiver) {
        if (shouldQualify && implicitReceiver != null) {
            return AstUtil.qualified(referencedName, implicitReceiver);
        } else {
            return referencedName.makeRef();
        }
    }
}
