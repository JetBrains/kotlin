package org.jetbrains.k2js.translate.intrinsic.array;

import com.google.dart.compiler.backend.js.ast.JsConditional;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;

import java.util.List;

/**
 * @author Talanov Pavel
 */
public enum ArrayGetIntrinsic implements FunctionIntrinsic {

    INSTANCE;

    @NotNull
    @Override
    public JsExpression apply(@NotNull JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert arguments.size() == 1 : "Array get expression must have one argument.";
        JsExpression indexExpression = arguments.get(0);
        TemporaryVariable arrayExpression = context.declareTemporary(receiver);
        JsConditional indexInBoundsCheck =
                IntrinsicArrayUtils.indexInBoundsCheck(indexExpression, arrayExpression, context);
        return AstUtil.newSequence(arrayExpression.assignmentExpression(), AstUtil.newArrayAccess(receiver, indexExpression));
    }
}
