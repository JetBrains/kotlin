package org.jetbrains.k2js.translate.intrinsic.array;

import com.google.dart.compiler.backend.js.ast.JsArrayLiteral;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;

import java.util.List;

/**
 * @author Talanov Pavel
 */
public enum ArrayNullConstructorIntrinsic implements FunctionIntrinsic {

    INSTANCE;

    //TODO: implement function passing to array constructor
    @NotNull
    @Override
    public JsExpression apply(@NotNull JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        return new JsArrayLiteral();
    }
}
