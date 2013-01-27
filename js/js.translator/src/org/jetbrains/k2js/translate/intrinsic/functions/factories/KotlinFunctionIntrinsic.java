package org.jetbrains.k2js.translate.intrinsic.functions.factories;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.List;

final class KotlinFunctionIntrinsic extends FunctionIntrinsic {
    @NotNull
    private final JsNameRef function;

    public KotlinFunctionIntrinsic(@NotNull String functionName) {
        function = new JsNameRef(functionName, Namer.KOTLIN_OBJECT_NAME);
    }

    @NotNull
    @Override
    public JsExpression apply(
            @Nullable JsExpression receiver,
            @NotNull List<JsExpression> arguments,
            @NotNull TranslationContext context
    ) {
        return new JsInvocation(function, receiver == null ? arguments : TranslationUtils.generateInvocationArguments(receiver, arguments));
    }
}
