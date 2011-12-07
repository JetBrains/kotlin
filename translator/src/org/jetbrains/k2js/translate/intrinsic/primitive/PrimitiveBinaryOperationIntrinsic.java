package org.jetbrains.k2js.translate.intrinsic.primitive;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.operation.OperatorTable;

import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class PrimitiveBinaryOperationIntrinsic implements FunctionIntrinsic {

    @NotNull
    public static PrimitiveBinaryOperationIntrinsic newInstance(@NotNull JetToken token) {
        JsBinaryOperator operator = OperatorTable.getBinaryOperator(token);
        return new PrimitiveBinaryOperationIntrinsic(operator);
    }

    @NotNull
    private final JsBinaryOperator operator;

    private PrimitiveBinaryOperationIntrinsic(@NotNull JsBinaryOperator operator) {
        this.operator = operator;
    }

    @NotNull
    @Override
    public JsExpression apply(@NotNull JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert arguments.size() == 1 : "Binary operator should have a receiver and one argument";
        return new JsBinaryOperation(operator, receiver, arguments.get(0));
    }
}
