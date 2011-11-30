package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.TranslationContext;
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
    public JsExpression translateSimpleName() {
        tryResolveAsPropertyAccess();
        tryResolveAsThisQualifiedExpression();
        tryResolveAsAliasReference();
        tryResolveAsGlobalReference();
        tryResolveAsLocalReference();
        if (result != null) {
            return result;
        }
        throw new AssertionError("Undefined name in this scope: " + simpleName.getReferencedName());
    }

    private void tryResolveAsThisQualifiedExpression() {
        if (alreadyResolved()) return;

        DeclarationDescriptor referencedDescriptor =
                BindingUtils.getDescriptorForReferenceExpression(context().bindingContext(), simpleName);

        if (referencedDescriptor == null) return;
        if (!context().isDeclared(referencedDescriptor)) return;

        JsName referencedName = context().getNameForDescriptor(referencedDescriptor);

        if (!requiresThisQualifier(simpleName, referencedName)) return;

        if (BindingUtils.isOwnedByClass(referencedDescriptor)) {
            result = TranslationUtils.getThisQualifiedNameReference(context(), referencedName);
        }
        if (BindingUtils.isOwnedByNamespace(referencedDescriptor)) {
            result = TranslationUtils.getQualifiedReference(context(), referencedDescriptor);
        }
    }

    private boolean requiresThisQualifier(@NotNull JetSimpleNameExpression expression,
                                          @NotNull JsName referencedName) {

        JsName name = context().enclosingScope().findExistingName(referencedName.getIdent());
        boolean isClassMember = context().classScope().ownsName(name);
        boolean isBackingFieldAccess = expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER;
        return (isBackingFieldAccess || isClassMember);
    }

    private void tryResolveAsAliasReference() {
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

    private void tryResolveAsGlobalReference() {
        if (alreadyResolved()) return;

        DeclarationDescriptor referencedDescriptor =
                BindingUtils.getDescriptorForReferenceExpression(context().bindingContext(), simpleName);

        if (referencedDescriptor == null) return;
        if (!context().isDeclared(referencedDescriptor)) return;

        result = TranslationUtils.getQualifiedReference(context(), referencedDescriptor);
    }

    private void tryResolveAsLocalReference() {
        if (alreadyResolved()) return;

        String name = getReferencedName();
        JsName localReferencedName = TranslationUtils.getLocalReferencedName(context(), name);

        if (localReferencedName == null) return;

        result = localReferencedName.makeRef();
    }

    @NotNull
    private String getReferencedName() {
        String name = simpleName.getReferencedName();
        assert name != null : "SimpleNameExpression should reference a name";
        return name;
    }

}
