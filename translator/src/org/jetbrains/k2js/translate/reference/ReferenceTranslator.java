package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

/**
 * @author Talanov Pavel
 */
//TODO: implement state
public class ReferenceTranslator extends AbstractTranslator {

    @NotNull
    static public ReferenceTranslator newInstance(@NotNull TranslationContext context) {
        return new ReferenceTranslator(context);
    }

    private ReferenceTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    // TODO: refactor put the checks inside resolvers
    @NotNull
    public JsExpression translateSimpleName(@NotNull JetSimpleNameExpression expression) {
        JsExpression result = resolveAsAliasReference(expression);
        if (result != null) return result;

        result = resolveAsPropertyAccess(expression);
        if (result != null) return result;

        result = resolveAsGlobalReference(expression);
        if (result != null) return result;

        result = resolveAsLocalReference(expression);
        if (result != null) return result;

        throw new AssertionError("Undefined name in this scope: " + expression.getReferencedName());
    }

    @Nullable
    private JsNameRef resolveAsAliasReference(@NotNull JetSimpleNameExpression expression) {
        //TODO: decide if this code is meaningful
        DeclarationDescriptor referencedDescriptor =
                BindingUtils.getDescriptorForReferenceExpression(context().bindingContext(), expression);
        if (referencedDescriptor == null) return null;

        if (!context().aliaser().hasAliasForDeclaration(referencedDescriptor)) {
            return null;
        }

        return context().aliaser().getAliasForDeclaration(referencedDescriptor);
    }

    @Nullable
    private JsInvocation resolveAsPropertyAccess(@NotNull JetSimpleNameExpression expression) {
        PropertyAccessTranslator propertyAccessTranslator = Translation.propertyAccessTranslator(context());
        if (propertyAccessTranslator.canBePropertyGetterCall(expression)) {
            return propertyAccessTranslator.translateAsPropertyGetterCall(expression);
        }
        return null;
    }

    @Nullable
    private JsExpression resolveAsGlobalReference(@NotNull JetSimpleNameExpression expression) {
        DeclarationDescriptor referencedDescriptor =
                BindingUtils.getDescriptorForReferenceExpression(context().bindingContext(), expression);
        if (referencedDescriptor == null) {
            return null;
        }
        if (!context().isDeclared(referencedDescriptor)) {
            return null;
        }
        JsName referencedName = context().getNameForDescriptor(referencedDescriptor);
        return ReferenceProvider.getReference(referencedName, context(), expression);
    }

    @Nullable
    private JsExpression resolveAsLocalReference(@NotNull JetSimpleNameExpression expression) {
        String name = expression.getReferencedName();
        assert name != null : "SimpleNameExpression should reference a name";
        JsName localReferencedName = TranslationUtils.getLocalReferencedName
                (context(), name);
        if (localReferencedName == null) {
            return null;
        }
        return localReferencedName.makeRef();
    }

}
