package org.jetbrains.k2js.translate.intrinsic.primitive;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsPrefixOperation;
import com.google.dart.compiler.backend.js.ast.JsUnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.operation.OperatorTable;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class PrimitiveUnaryOperationIntrinsic implements FunctionIntrinsic {

    @NotNull
    public static PrimitiveUnaryOperationIntrinsic newInstance(@NotNull JetToken token) {
        JsUnaryOperator operator = OperatorTable.getUnaryOperator(token);
        return new PrimitiveUnaryOperationIntrinsic(operator);
    }

    @NotNull
    private final JsUnaryOperator operator;

    private PrimitiveUnaryOperationIntrinsic(@NotNull JsUnaryOperator operator) {
        this.operator = operator;
    }

    @NotNull
    @Override
    public JsExpression apply(@NotNull JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert arguments.size() == 0 : "Unary operator should not have arguments.";
        //NOTE: cannot use this for increment/decrement
        return new JsPrefixOperation(operator, receiver);
    }
}
