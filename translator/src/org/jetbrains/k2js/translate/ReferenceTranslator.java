package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author Talanov Pavel
 */
public class ReferenceTranslator extends AbstractTranslator {

    @NotNull
    static public ReferenceTranslator newInstance(@NotNull TranslationContext context) {
        return new ReferenceTranslator(context);
    }

    private ReferenceTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @NotNull
    JsExpression translateSimpleName(@NotNull JetSimpleNameExpression expression) {
        //TODO: this is only a hack for now
        // Problem is that namespace properties do not generate getters and setter actually so they must be referenced
        // by name
        JsExpression result;
        result = resolveAsGlobalReference(expression);
        if (result != null) {
            return result;
        }
        result = resolveAsLocalReference(expression);
        if (result != null) {
            return result;
        }
        JsInvocation getterCall =
                Translation.propertyAccessTranslator(translationContext()).resolveAsPropertyGet(expression);
        if (getterCall != null) {
            return getterCall;
        }
        throw new AssertionError("Undefined name in this scope: " + expression.getReferencedName());

    }

    @Nullable
    private JsExpression resolveAsGlobalReference(@NotNull JetSimpleNameExpression expression) {
        DeclarationDescriptor referencedDescriptor =
                BindingUtils.getDescriptorForReferenceExpression(translationContext().bindingContext(), expression);
        if (referencedDescriptor == null) {
            return null;
        }
        if (!translationContext().isDeclared(referencedDescriptor)) {
            return null;
        }
        JsName referencedName = translationContext().getNameForDescriptor(referencedDescriptor);
        return generateCorrectReference(expression, referencedName);
    }

    @Nullable
    private JsExpression resolveAsLocalReference(@NotNull JetSimpleNameExpression expression) {
        JsName localReferencedName = getLocalReferencedName(expression);
        if (localReferencedName == null) {
            return null;
        }
        return generateCorrectReference(expression, localReferencedName);
    }

    @NotNull
    private JsNameRef generateCorrectReference(@NotNull JetSimpleNameExpression expression,
                                               @NotNull JsName referencedName) {
        JsNameRef result;
        if (requiresNamespaceQualifier(referencedName)) {
            result = translationContext().getNamespaceQualifiedReference(referencedName);
        } else {
            result = referencedName.makeRef();
            if (requiresThisQualifier(expression)) {
                result.setQualifier(new JsThisRef());
            }
        }
        return result;
    }

    private boolean requiresNamespaceQualifier(@NotNull JsName referencedName) {
        return translationContext().namespaceScope().ownsName(referencedName);
    }

    private boolean requiresThisQualifier(@NotNull JetSimpleNameExpression expression) {
        boolean isClassMember = translationContext().classScope().ownsName(getLocalReferencedName(expression));
        boolean isBackingFieldAccess = expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER;
        return isClassMember || isBackingFieldAccess;
    }

    @Nullable
    private JsName getLocalReferencedName(@NotNull JetSimpleNameExpression expression) {
        String referencedName = expression.getReferencedName();
        return translationContext().enclosingScope().findExistingName(referencedName);
    }


}
