package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.general.TranslationContext;

/**
 * @author Talanov Pavel
 */
public final class IntrinsicBinaryOperationTranslator extends BinaryOperationTranslator {

    protected IntrinsicBinaryOperationTranslator(@NotNull JetBinaryExpression expression,
                                                 @NotNull TranslationContext context) {
        super(expression, context);
    }

    @NotNull
    @Override
    protected JsExpression translate() {
        return asBinaryOperation();
    }

    //TODO: refactor
    @NotNull
    private JsExpression asBinaryOperation() {
        if (isPropertyOnTheLeft && OperatorTable.isAssignment(getOperationToken())) {
            return setterCall(right);
        }
        JetToken token = getOperationToken();
        if (OperatorTable.hasCorrespondingBinaryOperator(token)) {
            return new JsBinaryOperation(OperatorTable.getBinaryOperator(token), left, right);
        }
        //TODO: implement using intrinsic mechanism
//        if (OperatorTable.hasCorrespondingFunctionInvocation(token)) {
//            JsInvocation functionInvocation = OperatorTable.getCorrespondingFunctionInvocation(token);
//            functionInvocation.setArguments(left, right);
//            return functionInvocation;
//        }
        throw new AssertionError("Unsupported token encountered: " + token.toString());
    }

}
