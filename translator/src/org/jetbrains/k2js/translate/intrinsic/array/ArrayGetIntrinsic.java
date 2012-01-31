package org.jetbrains.k2js.translate.intrinsic.array;

import com.google.dart.compiler.backend.js.ast.JsArrayAccess;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public enum ArrayGetIntrinsic implements FunctionIntrinsic {

    INSTANCE;

    @NotNull
    @Override
    public JsExpression apply(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert receiver != null;
        assert arguments.size() == 1 : "Array get expression must have one argument.";
        JsExpression indexExpression = arguments.get(0);
//        TemporaryVariable arrayExpression = context.declareTemporary(receiver);
//        JsConditional indexInBoundsCheck =
//                IntrinsicArrayUtils.indexInBoundsCheck(indexExpression, arrayExpression, context);
//        return AstUtil.newSequence(arrayExpression.assignmentExpression(), AstUtil.newArrayAccess(receiver, indexExpression));
        return new JsArrayAccess(receiver, indexExpression);
    }
}
