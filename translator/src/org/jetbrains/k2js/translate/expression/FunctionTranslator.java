package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class FunctionTranslator extends AbstractTranslator {

    @NotNull
    private final JetDeclarationWithBody functionDeclaration;
    @NotNull
    private final JsFunction functionObject;

    @NotNull
    public static FunctionTranslator newInstance(@NotNull JetDeclarationWithBody function,
                                                 @NotNull TranslationContext context) {
        return new FunctionTranslator(function, context);
    }

    private FunctionTranslator(@NotNull JetDeclarationWithBody functionDeclaration,
                               @NotNull TranslationContext context) {
        super(context);
        this.functionDeclaration = functionDeclaration;
        this.functionObject = createFunctionObject();
    }

    @NotNull
    public JsPropertyInitializer translateAsMethod() {
        assert functionDeclaration instanceof JetElement;
        JsName functionName = context().getNameForElement((JetElement) functionDeclaration);
        JsFunction function = generateFunctionObject();
        return new JsPropertyInitializer(functionName.makeRef(), function);
    }

    //TODO: consider refactoring
    @NotNull
    public JsExpression translateAsLiteral() {
        //TODO: provide a way not to pollute global namespace
        JsName aliasForThis = context().enclosingScope().declareName("that");
        context().aliaser().setAliasForThis(aliasForThis);
        JsFunction function = generateFunctionObject();
        context().aliaser().removeAliasForThis();
        JsExpression assignThatToThis = AstUtil.newAssignment(aliasForThis.makeRef(), new JsThisRef());
        return AstUtil.newSequence(assignThatToThis, function);
    }

    @NotNull
    private JsFunction generateFunctionObject() {
        functionObject.setParameters(translateParameters(functionDeclaration.getValueParameters(),
                functionObject.getScope()));
        functionObject.setBody(translateBody());
        return functionObject;
    }

    private JsFunction createFunctionObject() {
        if (isDeclaration()) {
            return JsFunction.getAnonymousFunctionWithScope
                    (context().getScopeForElement((JetDeclaration) functionDeclaration));
        }
        if (isLiteral()) {
            return new JsFunction(context().enclosingScope());
        }
        throw new AssertionError("Unsupported type of functionDeclaration.");
    }

    private boolean isLiteral() {
        return functionDeclaration instanceof JetFunctionLiteral;
    }

    private boolean isDeclaration() {
        return (functionDeclaration instanceof JetNamedFunction) ||
                (functionDeclaration instanceof JetPropertyAccessor);
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
        if (functionDeclaration instanceof JetPropertyAccessor) {
            return context().newPropertyAccess((JetPropertyAccessor) functionDeclaration);
        }
        if (isLiteral()) {
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
