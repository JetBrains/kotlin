package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
import org.jetbrains.k2js.translate.general.TranslationContext;

/**
 * @author Talanov Pavel
 */
public final class OverloadedUnaryOperationTranslator extends UnaryOperationTranslator {

    @NotNull
    public static JsExpression translate(@NotNull JetUnaryExpression expression,
                                         @NotNull TranslationContext context) {
        return (new OverloadedUnaryOperationTranslator(expression, context))
                .translate();
    }

    @NotNull
    private final JsNameRef operationReference;

    private OverloadedUnaryOperationTranslator(@NotNull JetUnaryExpression expression,
                                               @NotNull TranslationContext context) {
        super(expression, context);
        this.operationReference = getOverloadedOperationReference(expression, context);
    }

    @Override
    @NotNull
    protected JsExpression translate() {
        return translateAsMethodCall();
    }

    @Override
    @NotNull
    protected JsExpression operationExpression(@NotNull JsExpression receiver) {
        AstUtil.setQualifier(operationReference, receiver);
        return AstUtil.newInvocation(operationReference);
    }

}
