package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.expression.QualifiedExpressionTranslator;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

/**
 * @author Pavel Talanov
 */
public abstract class AccessTranslator extends AbstractTranslator {

    @NotNull
    public static AccessTranslator getAccessTranslator(@NotNull JetExpression referenceExpression,
                                                       @NotNull TranslationContext context) {
        assert ((referenceExpression instanceof JetReferenceExpression) ||
                (referenceExpression instanceof JetDotQualifiedExpression));
        //TODO:
        if (PropertyAccessTranslator.canBePropertyAccess(referenceExpression, context)) {
            if (referenceExpression instanceof JetQualifiedExpression) {
                return QualifiedExpressionTranslator.getAccessTranslator((JetQualifiedExpression) referenceExpression, context);
            }
            assert referenceExpression instanceof JetSimpleNameExpression;
            return PropertyAccessTranslator.newInstance((JetSimpleNameExpression) referenceExpression, null, context);
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
