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

    @Nullable
    private JsExpression result;

    private ReferenceTranslator(@NotNull DeclarationDescriptor referencedDescriptor,
                                boolean shouldQualify,
                                @NotNull TranslationContext context) {
        super(context);
        this.referencedDescriptor = referencedDescriptor;
        this.result = null;
        this.shouldQualify = shouldQualify;
    }

    @NotNull
    public JsExpression translate() {
        tryResolveAsAlias();
        tryResolveAsReference();
        if (result != null) {
            return result;
        }
        throw new AssertionError("Undefined name in this scope: " + referencedDescriptor.getName());
    }

    private void tryResolveAsAlias() {
        if (alreadyResolved()) return;

        if (!context().aliaser().hasAliasForDeclaration(referencedDescriptor)) return;

        result = context().aliaser().getAliasForDeclaration(referencedDescriptor);
    }


    private void tryResolveAsReference() {
        if (alreadyResolved()) return;

        if (!context().isDeclared(referencedDescriptor)) return;

        JsName referencedName = context().getNameForDescriptor(referencedDescriptor);
        JsExpression implicitReceiver = getImplicitReceiver(context(), referencedDescriptor);

        if (shouldQualify && implicitReceiver != null) {
            result = AstUtil.qualified(referencedName, implicitReceiver);
        } else {
            result = referencedName.makeRef();
        }
    }

    private boolean alreadyResolved() {
        return result != null;
    }
}
