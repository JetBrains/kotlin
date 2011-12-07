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

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;

/**
 * @author Talanov Pavel
 */
public class ReferenceTranslator extends AbstractTranslator {

    // TODO: move to other util
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

    @NotNull
    public static JsExpression translateSimpleName(@NotNull JetSimpleNameExpression expression,
                                                   @NotNull TranslationContext context) {
        if (PropertyAccessTranslator.canBePropertyAccess(expression, context)) {
            return PropertyAccessTranslator.translateAsPropertyGetterCall(expression, context);
        }
        DeclarationDescriptor referencedDescriptor =
                getDescriptorForReferenceExpression(context.bindingContext(), expression);
        return (new ReferenceTranslator(referencedDescriptor, context)).translate();
    }

    @NotNull
    public static JsExpression translateReference(@NotNull DeclarationDescriptor referencedDescriptor,
                                                  @NotNull TranslationContext context) {
        return (new ReferenceTranslator(referencedDescriptor, context)).translate();
    }

    @NotNull
    private final DeclarationDescriptor referencedDescriptor;

    @Nullable
    private JsExpression result;

    private ReferenceTranslator(@NotNull DeclarationDescriptor referencedDescriptor, @NotNull TranslationContext context) {
        super(context);
        this.referencedDescriptor = referencedDescriptor;
        this.result = null;
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
        JsExpression implicitReceiver = getImplicitReceiver(referencedDescriptor, context());

        generateReference(referencedName, implicitReceiver);
    }

    private void generateReference(@NotNull JsName referencedName, @Nullable JsExpression implicitReceiver) {
        if (implicitReceiver != null) {
            result = AstUtil.qualified(referencedName, implicitReceiver);
        } else {
            result = context().getNameForDescriptor(referencedDescriptor).makeRef();
        }
    }

    private boolean alreadyResolved() {
        return result != null;
    }
}
