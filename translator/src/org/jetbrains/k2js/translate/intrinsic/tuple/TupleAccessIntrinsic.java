package org.jetbrains.k2js.translate.intrinsic.tuple;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class TupleAccessIntrinsic implements FunctionIntrinsic {

    private final int elementIndex;

    public TupleAccessIntrinsic(int elementIndex) {
        this.elementIndex = elementIndex;
    }

    @NotNull
    @Override
    public JsExpression apply(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert arguments.isEmpty() : "Tuple access expression should not have any arguments.";
        return AstUtil.newArrayAccess(receiver, context.program().getNumberLiteral(elementIndex));
    }
}
