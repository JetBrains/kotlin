package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetPrefixExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.reference.PropertyAccessTranslator;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import static org.jetbrains.k2js.translate.utils.BindingUtils.isVariableReassignment;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.getBaseExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateBaseExpression;

/**
 * @author Talanov Pavel
 */
//TODO: reexamine class, see if can be clearer
public abstract class UnaryOperationTranslator extends OperationTranslator {

    @NotNull
    private final JetUnaryExpression expression;
    @NotNull
    protected final JsExpression baseExpression;
    protected final boolean isPrefix;
    private final boolean isVariableReassignment;
    private final boolean isStatement;
    protected final boolean isPropertyAccess;

    protected UnaryOperationTranslator(@NotNull JetUnaryExpression expression,
                                       @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.isPrefix = isPrefix(expression);
        this.isVariableReassignment = isVariableReassignment(context.bindingContext(), expression);
        this.isStatement = BindingUtils.isStatement(context().bindingContext(), expression);
        this.baseExpression = translateBaseExpression(context, expression);
        this.isPropertyAccess = isPropertyAccess(getBaseExpression(expression));
    }

    @NotNull
    protected abstract JsExpression translate();

    @NotNull
    protected JsExpression translateAsMethodCall() {
        if (isStatement || isPrefix) {
            return asPrefix();
        }
        if (isVariableReassignment) {
            return asPostfixWithReassignment();
        } else {
            return asPostfixWithNoReassignment();
        }
    }

    @NotNull
    private JsExpression asPrefix() {
        if (isVariableReassignment) {
            return variableReassignment(baseExpression);
        }
        return operationExpression(baseExpression);
    }

    //TODO: decide if this expression can be optimised in case of direct access (not property)
    @NotNull
    private JsExpression asPostfixWithReassignment() {
        // code fragment: expr(a++)
        // generate: expr( (t1 = a, t2 = t1, a = t1.inc(), t2) )
        TemporaryVariable t1 = declareTemporary(baseExpression);
        TemporaryVariable t2 = declareTemporary(t1.nameReference());
        JsExpression variableReassignment = variableReassignment(t1.nameReference());
        return AstUtil.newSequence(t1.assignmentExpression(), t2.assignmentExpression(),
                variableReassignment, t2.nameReference());
    }

    @NotNull
    private JsExpression asPostfixWithNoReassignment() {
        // code fragment: expr(a++)
        // generate: expr( (t1 = a, t2 = t1, t2.inc(), t1) )
        TemporaryVariable t1 = declareTemporary(baseExpression);
        TemporaryVariable t2 = declareTemporary(t1.nameReference());
        JsExpression methodCall = operationExpression(t2.nameReference());
        JsExpression returnedValue = t1.nameReference();
        return AstUtil.newSequence(t1.assignmentExpression(), t2.assignmentExpression(), methodCall, returnedValue);
    }

    @NotNull
    private JsExpression variableReassignment(@NotNull JsExpression toCallMethodUpon) {
        if (isPropertyAccess) {
            return propertyReassignment(toCallMethodUpon);
        }
        return localVariableReassignment(toCallMethodUpon);
    }

    private JsExpression localVariableReassignment(@NotNull JsExpression toCallMethodUpon) {
        assert baseExpression instanceof JsNameRef : "Base expression should be an l-value";
        return AstUtil.newAssignment((JsNameRef) baseExpression, operationExpression(toCallMethodUpon));
    }

    @NotNull
    private JsExpression propertyReassignment(@NotNull JsExpression toCallMethodUpon) {
        JetExpression jetBaseExpression = getBaseExpression(expression);
        JsInvocation setterCall =
                PropertyAccessTranslator.translateAsPropertySetterCall(jetBaseExpression, context());
        assert PropertyAccessTranslator.canBePropertyGetterCall(jetBaseExpression, context()) : "Should be a getter call";
        JsExpression overloadedMethodCallOnPropertyGetter = operationExpression(toCallMethodUpon);
        setterCall.setArguments(overloadedMethodCallOnPropertyGetter);
        return setterCall;
    }

    @NotNull
    abstract JsExpression operationExpression(@NotNull JsExpression receiver);

    @NotNull
    protected JetToken getOperationToken() {
        JetSimpleNameExpression operationExpression = expression.getOperation();
        IElementType elementType = operationExpression.getReferencedNameElementType();
        assert elementType instanceof JetToken : "Unary expression should have IElementType of type JetToken";
        return (JetToken) elementType;
    }

    private boolean isPrefix(JetUnaryExpression expression) {
        return (expression instanceof JetPrefixExpression);
    }


}
