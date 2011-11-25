package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class FunctionTranslator extends AbstractTranslator {

    @NotNull
    private final JetFunction functionDeclaration;
    @NotNull
    private final JsFunction functionObject;

    @NotNull
    public static FunctionTranslator newInstance(@NotNull JetFunction function, @NotNull TranslationContext context) {
        return new FunctionTranslator(function, context);
    }

    private FunctionTranslator(@NotNull JetFunction functionDeclaration, @NotNull TranslationContext context) {
        super(context);
        this.functionDeclaration = functionDeclaration;
        this.functionObject = createFunctionObject();
    }

    @NotNull
    public JsPropertyInitializer translateAsMethod() {
        JsName functionName = context().getNameForElement(functionDeclaration);
        JsFunction function = generateFunctionObject();
        return new JsPropertyInitializer(functionName.makeRef(), function);
    }

    @NotNull
    public JsFunction translateAsLiteral() {
        return generateFunctionObject();
    }

    @NotNull
    private JsFunction generateFunctionObject() {
        functionObject.setParameters(translateParameters(functionDeclaration.getValueParameters(),
                functionObject.getScope()));
        functionObject.setBody(translateBody());
        return functionObject;
    }

    private JsFunction createFunctionObject() {
        if (functionDeclaration instanceof JetNamedFunction) {
            return JsFunction.getAnonymousFunctionWithScope
                    (context().getScopeForElement(functionDeclaration));
        }
        if (functionDeclaration instanceof JetFunctionLiteral) {
            return new JsFunction(context().enclosingScope());
        }
        throw new AssertionError("Unsupported type of functionDeclaration.");
    }

    @NotNull
    private JsBlock translateBody() {
        JetExpression jetBodyExpression = functionDeclaration.getBodyExpression();
        //TODO decide if there are cases where this assert is illegal
        assert jetBodyExpression != null : "Function without body not supported";
        JsNode body = Translation.translateExpression(jetBodyExpression, functionBodyContext());
        return wrapWithReturnIfNeeded(body, !functionDeclaration.hasBlockBody());
    }

    @NotNull
    private JsBlock wrapWithReturnIfNeeded(@NotNull JsNode body, boolean needsReturn) {
        if (!needsReturn) {
            return AstUtil.convertToBlock(body);
        }
        if (body instanceof JsExpression) {
            return AstUtil.convertToBlock(new JsReturn(AstUtil.convertToExpression(body)));
        }
        if (body instanceof JsBlock) {
            JsBlock bodyBlock = (JsBlock) body;
            addReturnToBlockStatement(bodyBlock);
            return bodyBlock;
        }
        throw new AssertionError("Invalid node as functionDeclaration body");
    }

    private void addReturnToBlockStatement(@NotNull JsBlock bodyBlock) {
        List<JsStatement> statements = bodyBlock.getStatements();
        int lastIndex = statements.size() - 1;
        JsStatement lastStatement = statements.get(lastIndex);
        statements.set(lastIndex,
                new JsReturn(AstUtil.extractExpressionFromStatement(lastStatement)));
    }

    @NotNull
    private TranslationContext functionBodyContext() {
        if (functionDeclaration instanceof JetNamedFunction) {
            return context().newFunctionDeclaration((JetNamedFunction) functionDeclaration);
        }
        if (functionDeclaration instanceof JetFunctionLiteral) {
            return context().newEnclosingScope(functionObject.getScope());
        }
        throw new AssertionError("Unsupported type of functionDeclaration.");
    }

    @NotNull
    private List<JsParameter> translateParameters(@NotNull List<JetParameter> jetParameters,
                                                  @NotNull JsScope functionScope) {
        List<JsParameter> jsParameters = new ArrayList<JsParameter>();
        for (JetParameter jetParameter : jetParameters) {
            JsName parameterName = functionScope.declareName(jetParameter.getName());
            jsParameters.add(new JsParameter(parameterName));
        }
        return jsParameters;
    }
}
