package org.jetbrains.k2js.translate.intrinsic.primitive;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.CompareToIntrinsic;
import org.jetbrains.k2js.translate.operation.OperatorTable;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class PrimitiveCompareToIntrinsic extends CompareToIntrinsic {

    @NotNull
    public static PrimitiveCompareToIntrinsic newInstance() {
        return new PrimitiveCompareToIntrinsic();
    }

    private PrimitiveCompareToIntrinsic() {
    }

    @NotNull
    public JsExpression apply(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert arguments.size() == 1 : "Equals operation should have one argument";
        JsBinaryOperator operator = OperatorTable.getBinaryOperator(getComparisonToken());
        JsExpression argument = arguments.get(0);
        return new JsBinaryOperation(operator, receiver, argument);
    }

}
