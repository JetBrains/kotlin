package org.jetbrains.k2js.translate.intrinsic.array;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static org.jetbrains.k2js.translate.utils.TranslationUtils.zeroLiteral;

//TODO: decide if needed

/**
 * @author Pavel Talanov
 */
public final class IntrinsicArrayUtils {

    private IntrinsicArrayUtils() {
    }

    @NotNull
    public static JsConditional indexInBoundsCheck(@NotNull JsExpression indexExpression,
                                                   @NotNull TemporaryVariable arrayTemporary,
                                                   @NotNull TranslationContext context) {
        JsExpression positiveOrZero = new JsBinaryOperation
                (JsBinaryOperator.GTE, indexExpression, zeroLiteral(context));
        JsNameRef lengthExpression = AstUtil.newQualifiedNameRef("length");
        lengthExpression.setQualifier(arrayTemporary.nameReference());
        JsExpression lessThanLength = new JsBinaryOperation
                (JsBinaryOperator.LT, indexExpression, lengthExpression);
        JsExpression indexInBounds = AstUtil.and(positiveOrZero, lessThanLength);
        JsConditional indexCheck = new JsConditional();
        indexCheck.setTestExpression(indexInBounds);
        indexCheck.setElseExpression(throwOutOfBoundsException());
        return indexCheck;
    }

    @NotNull
    private static JsExpression throwOutOfBoundsException() {
        //TODO: think about exception
        return AstUtil.newQualifiedNameRef("ErrorName");
    }
}
