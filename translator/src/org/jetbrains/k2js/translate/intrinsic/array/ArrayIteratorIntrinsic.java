package org.jetbrains.k2js.translate.intrinsic.array;

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
public enum ArrayIteratorIntrinsic implements FunctionIntrinsic {

    INSTANCE;

    @NotNull
    @Override
    public JsExpression apply(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert receiver != null;
        assert arguments.size() == 0;
        //TODO: provide better mechanism
        JsNameRef iteratorFunName = AstUtil.newQualifiedNameRef("Kotlin.arrayIterator");
        return AstUtil.newInvocation(iteratorFunName, receiver);
    }
}
