package org.jetbrains.k2js.translate.intrinsic.array;

import com.google.dart.compiler.backend.js.ast.JsArrayAccess;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;

import java.util.List;

/**
 * @author Talanov Pavel
 */
public enum ArraySetIntrinsic implements FunctionIntrinsic {

    INSTANCE;

    @NotNull
    @Override
    public JsExpression apply(@NotNull JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert arguments.size() == 2 : "Array set expression must have two arguments.";
        JsExpression indexExpression = arguments.get(0);
        JsExpression value = arguments.get(1);
        JsArrayAccess arrayAccess = AstUtil.newArrayAccess(receiver, indexExpression);
        return AstUtil.newAssignment(arrayAccess, value);
    }
}
