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
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

/**
 * @author Talanov Pavel
 */
public class ReferenceTranslator extends AbstractTranslator {

    @NotNull
    private final JetSimpleNameExpression simpleName;

    @Nullable
    private JsExpression result;

    @NotNull
    public static JsExpression translateSimpleName(@NotNull JetSimpleNameExpression expression,
                                                   @NotNull TranslationContext context) {
        return (new ReferenceTranslator(expression, context)).translateSimpleName();
    }

    private ReferenceTranslator(@NotNull JetSimpleNameExpression expression, @NotNull TranslationContext context) {
        super(context);
        this.simpleName = expression;
        this.result = null;
    }

    @NotNull
    //TODO: make this process simpler and clearer
    public JsExpression translateSimpleName() {
        tryResolveAsAlias();
        tryResolveAsPropertyAccess();
        tryResolveAsReference();
        if (result != null) {
            return result;
        }
        throw new AssertionError("Undefined name in this scope: " + simpleName.getReferencedName());
    }

    //TODO: refactor
    private void tryResolveAsReference() {
        if (alreadyResolved()) return;

        DeclarationDescriptor referencedDescriptor =
                BindingUtils.getDescriptorForReferenceExpression(context().bindingContext(), simpleName);

        if (referencedDescriptor == null) return;
        if (!context().isDeclared(referencedDescriptor)) return;

        JsName referencedName = context().getNameForDescriptor(referencedDescriptor);
        JsExpression implicitReceiver = getImplicitReceiver(referencedDescriptor, context());

        if (implicitReceiver != null) {
            result = AstUtil.qualified(referencedName, implicitReceiver);
        } else {
            result = context().getNameForDescriptor(referencedDescriptor).makeRef();
        }
    }

    @Nullable
    public static JsExpression getImplicitReceiver(@NotNull DeclarationDescriptor referencedDescriptor,
                                                   @NotNull TranslationContext context) {
        if (!context.isDeclared(referencedDescriptor)) return null;

        if (BindingUtils.isOwnedByClass(referencedDescriptor)) {
            return TranslationUtils.getThisQualifier(context);
        }
        if (!BindingUtils.isOwnedByNamespace(referencedDescriptor)) return null;

        return context.declarations().getQualifier(referencedDescriptor);
    }

    private void tryResolveAsAlias() {
        //TODO: decide if this code is meaningful
        if (alreadyResolved()) return;

        DeclarationDescriptor referencedDescriptor =
                BindingUtils.getDescriptorForReferenceExpression(context().bindingContext(), simpleName);

        if (referencedDescriptor == null) return;
        if (!context().aliaser().hasAliasForDeclaration(referencedDescriptor)) return;

        result = context().aliaser().getAliasForDeclaration(referencedDescriptor);
    }

    private boolean alreadyResolved() {
        return result != null;
    }

    private void tryResolveAsPropertyAccess() {
        if (alreadyResolved()) return;

        if (!PropertyAccessTranslator.canBePropertyGetterCall(simpleName, context())) return;

        result = PropertyAccessTranslator.translateAsPropertyGetterCall(simpleName, context());
    }
}
