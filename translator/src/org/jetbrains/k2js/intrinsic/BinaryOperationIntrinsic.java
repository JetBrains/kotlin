package org.jetbrains.k2js.intrinsic;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsThisRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class BinaryOperationIntrinsic implements Intrinsic {
    @NotNull
    @Override
    public JsExpression apply(@NotNull FunctionDescriptor descriptor, @NotNull JetExpression receiver,
                              @NotNull List<JetExpression> arguments) {
        //TODO: implement
//
//        JetToken correspondingToken = OperatorConventions.getTokenForMethodName(descriptor.getName());
//        JsBinaryOperator operator = OperatorTable.getBinaryOperator(correspondingToken);
//
//        assert arguments.size() == 1 : "Binary operations expects 2 arguments.";
//        JsExpression argument = arguments.get(0);
//        assert  argument != null;
//
//        return new JsBinaryOperation(operator, receiver, argument);
        return new JsThisRef();
    }
}
