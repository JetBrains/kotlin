package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.general.TranslationContext;

/**
 * @author Talanov Pavel
 */
public final class ReferenceProvider {

    @NotNull
    private final TranslationContext context;
    @NotNull
    private final JsName referencedName;
    private boolean isBackingFieldAccess;
    private boolean requiresThisQualifier;
    private boolean requiresNamespaceQualifier;

    public static JsNameRef getReference(@NotNull JsName referencedName, @NotNull TranslationContext context,
                                         boolean isBackingFieldAccess) {
        return (new ReferenceProvider(referencedName, context, isBackingFieldAccess)).generateCorrectReference();
    }


    public static JsNameRef getReference(@NotNull JsName referencedName, @NotNull TranslationContext context,
                                         JetSimpleNameExpression expression) {
        boolean isBackingFieldAccess = expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER;
        return (new ReferenceProvider(referencedName, context, isBackingFieldAccess))
                .generateCorrectReference();
    }

    private ReferenceProvider(@NotNull JsName referencedName, @NotNull TranslationContext context,
                              boolean isBackingFieldAccess) {
        this.context = context;
        this.referencedName = referencedName;
        this.isBackingFieldAccess = isBackingFieldAccess;
        this.requiresThisQualifier = requiresThisQualifier();
        this.requiresNamespaceQualifier = requiresNamespaceQualifier();
    }

    @NotNull
    public JsNameRef generateCorrectReference() {
        if (requiresNamespaceQualifier) {
            return context.getNamespaceQualifiedReference(referencedName);
        } else if (requiresThisQualifier) {
            return AstUtil.thisQualifiedReference(referencedName);
        }
        return referencedName.makeRef();
    }

    private boolean requiresNamespaceQualifier() {
        return context.namespaceScope().ownsName(referencedName);
    }

    private boolean requiresThisQualifier() {
        JsName name = context.enclosingScope().findExistingName(referencedName.getIdent());
        boolean isClassMember = context.classScope().ownsName(name);
        return isClassMember || isBackingFieldAccess;
    }
}
