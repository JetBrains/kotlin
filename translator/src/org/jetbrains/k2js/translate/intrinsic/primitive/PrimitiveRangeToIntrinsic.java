package org.jetbrains.k2js.translate.intrinsic.primitive;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBooleanLiteral;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNew;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;

import java.util.Arrays;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class PrimitiveRangeToIntrinsic implements FunctionIntrinsic {

    @NotNull
    public static PrimitiveRangeToIntrinsic newInstance() {
        return new PrimitiveRangeToIntrinsic();
    }

    private PrimitiveRangeToIntrinsic() {
    }

    @NotNull
    @Override
    public JsExpression apply(@NotNull JsExpression rangeStart, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert arguments.size() == 1 : "RangeTo must have one argument.";
        JsExpression rangeEnd = arguments.get(0);
        JsBinaryOperation rangeSize = AstUtil.sum(AstUtil.subtract(rangeEnd, rangeStart),
                context.program().getNumberLiteral(1));
        //TODO: provide a way not to hard code this value
        JsNew numberRangeConstructorInvocation
                = new JsNew(AstUtil.newQualifiedNameRef("Kotlin.NumberRange"));
        //TODO: add tests and correct expression for reversed ranges.
        JsBooleanLiteral isRangeReversed = context.program().getFalseLiteral();
        numberRangeConstructorInvocation.setArguments(Arrays.asList(rangeStart, rangeSize, isRangeReversed));
        return numberRangeConstructorInvocation;
    }
}
