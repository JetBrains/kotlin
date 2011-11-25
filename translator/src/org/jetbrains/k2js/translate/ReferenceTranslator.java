package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;

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
        JsExpression result = resolveAsPropertyAccess(expression);
        if (result != null) {
            return result;
        }
        result = resolveAsGlobalReference(expression);
        if (result != null) {
            return result;
        }
        result = resolveAsLocalReference(expression);
        if (result != null) {
            return result;
        }
        throw new AssertionError("Undefined name in this scope: " + expression.getReferencedName());

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
        return TranslationUtils.getReference(context(), expression, referencedName);
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
