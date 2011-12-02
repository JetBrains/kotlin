package org.jetbrains.k2js.translate.intrinsic;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsPrefixOperation;
import com.google.dart.compiler.backend.js.ast.JsUnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.operation.OperatorTable;

import java.util.List;

/**
 * @author Talanov Pavel
 */
public class UnaryOperationIntrinsic implements Intrinsic {

    @NotNull
    /*package*/ static UnaryOperationIntrinsic newInstance(@NotNull JetToken token) {
        JsUnaryOperator operator = OperatorTable.getUnaryOperator(token);
        return new UnaryOperationIntrinsic(operator);
    }

    @NotNull
    private final JsUnaryOperator operator;

    private UnaryOperationIntrinsic(@NotNull JsUnaryOperator operator) {
        this.operator = operator;
    }

    @NotNull
    @Override
    public JsExpression apply(@NotNull JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert arguments.size() == 0 : "Unary operator should not have arguments.";
        //TODO: note that we cannot use this for increment/decrement
        return new JsPrefixOperation(operator, receiver);
    }
}
