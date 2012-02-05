package org.jetbrains.k2js.translate.intrinsic.string;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public enum GetIntrinsic implements FunctionIntrinsic {

    INSTANCE;

    @NotNull
    @Override
    public JsExpression apply(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert receiver != null;
        assert arguments.size() == 1 : "get Char expression must have 1 arguments.";
        //TODO: provide better way
        JsNameRef charAtReference = AstUtil.newQualifiedNameRef("charAt");
        AstUtil.setQualifier(charAtReference, receiver);
        return AstUtil.newInvocation(charAtReference, arguments);
    }
}
