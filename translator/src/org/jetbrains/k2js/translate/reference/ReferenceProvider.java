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
    private boolean requiresThisQualifier;
    private boolean requiresNamespaceQualifier;


    public ReferenceProvider(@NotNull TranslationContext context,
                             @NotNull JetSimpleNameExpression expression,
                             @NotNull JsName referencedName) {
        this.context = context;
        this.referencedName = referencedName;
        this.requiresThisQualifier = requiresThisQualifier(expression);
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

    private boolean requiresThisQualifier(@NotNull JetSimpleNameExpression expression) {
        JsName name = context.enclosingScope().findExistingName(referencedName.getIdent());
        boolean isClassMember = context.classScope().ownsName(name);
        boolean isBackingFieldAccess = expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER;
        return isClassMember || isBackingFieldAccess;
    }
}
