package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

/**
 * @author Talanov Pavel
 */
public abstract class AccessTranslator extends AbstractTranslator {

    @NotNull
    public static AccessTranslator getAccessTranslator(@NotNull JetExpression referenceExpression,
                                                       @NotNull TranslationContext context) {
        assert ((referenceExpression instanceof JetReferenceExpression) ||
                (referenceExpression instanceof JetDotQualifiedExpression));
        if (PropertyAccessTranslator.canBePropertyAccess(referenceExpression, context)) {
            return PropertyAccessTranslator.newInstance(referenceExpression, context);
        }
        if (referenceExpression instanceof JetArrayAccessExpression) {
            return ArrayAccessTranslator.newInstance((JetArrayAccessExpression) referenceExpression, context);
        }
        return ReferenceAccessTranslator.newInstance((JetSimpleNameExpression) referenceExpression, context);
    }


    @NotNull
    public static JsExpression translateAsGet(@NotNull JetReferenceExpression expression,
                                              @NotNull TranslationContext context) {
        return (getAccessTranslator(expression, context)).translateAsGet();
    }


    protected AccessTranslator(@Deprecated TranslationContext context) {
        super(context);
    }

    public abstract JsExpression translateAsGet();

    public abstract JsExpression translateAsSet(@NotNull JsExpression setTo);

}
