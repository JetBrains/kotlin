package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.DescriptorUtils;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForElement;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptor;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.functionWithScope;


/**
 * @author Talanov Pavel
 */
public final class FunctionTranslator extends AbstractTranslator {

    @NotNull
    private final JetDeclarationWithBody functionDeclaration;
    @NotNull
    private final JsFunction functionObject;
    @NotNull
    private final TranslationContext functionBodyContext;

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
        this.functionBodyContext = functionBodyContext();
    }

    @NotNull
    public JsPropertyInitializer translateAsMethod() {
        assert functionDeclaration instanceof JetElement;
        JsName functionName = context().getNameForElement((JetElement) functionDeclaration);
        JsFunction function = generateFunctionObject();
        return new JsPropertyInitializer(functionName.makeRef(), function);
    }

    @NotNull
    public JsExpression translateAsLiteral() {
        TemporaryVariable aliasForThis = context().newAliasForThis();
        JsFunction function = generateFunctionObject();
        context().removeAliasForThis();
        return AstUtil.newSequence(aliasForThis.assignmentExpression(), function);
    }

    @NotNull
    private JsFunction generateFunctionObject() {
        functionObject.setParameters(translateParameters(functionDeclaration.getValueParameters(), functionBodyContext));
        functionObject.setBody(translateBody(functionBodyContext));
        restoreContext();
        return functionObject;
    }

    private void restoreContext() {
        if (isExtensionFunction()) {
            functionBodyContext.removeAliasForThis();
        }
    }

    private JsFunction createFunctionObject() {
        if (isDeclaration()) {
            return functionWithScope
                    (context().getScopeForElement((JetDeclaration) functionDeclaration));
        }
        if (isLiteral()) {
            //TODO: look into
            return new JsFunction(context().jsScope());
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
    private JsBlock translateBody(@NotNull TranslationContext functionBodyContext) {
        JetExpression jetBodyExpression = functionDeclaration.getBodyExpression();
        //TODO decide if there are cases where this assert is illegal
        assert jetBodyExpression != null : "Function without body not supported";
        JsNode body = Translation.translateExpression(jetBodyExpression, functionBodyContext);
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
            return context().innerJsScope(functionObject.getScope());
        }
        throw new AssertionError("Unsupported type of functionDeclaration.");
    }

    @NotNull
    private List<JsParameter> translateParameters(@NotNull List<JetParameter> jetParameters,
                                                  @NotNull TranslationContext functionBodyContext) {
        List<JsParameter> jsParameters = new ArrayList<JsParameter>();
        mayBeAddThisParameterForExtensionFunction(jsParameters);
        for (JetParameter jetParameter : jetParameters) {
            JsName parameterName = declareParameter(jetParameter, functionBodyContext);
            jsParameters.add(new JsParameter(parameterName));
        }
        return jsParameters;
    }

    @NotNull
    private JsName declareParameter(@NotNull JetParameter jetParameter,
                                    @NotNull TranslationContext functionBodyContext) {
        DeclarationDescriptor parameterDescriptor =
                getDescriptorForElement(functionBodyContext.bindingContext(), jetParameter);
        return context().declareLocalVariable(parameterDescriptor);
    }

    private void mayBeAddThisParameterForExtensionFunction(@NotNull List<JsParameter> jsParameters) {
        boolean isExtensionFunction = isExtensionFunction();
        if (isExtensionFunction) {
            JsName receiver = functionBodyContext.jsScope().declareName("receiver");
            context().aliaser().setAliasForThis(receiver);
            jsParameters.add(new JsParameter(receiver));
        }
    }

    private boolean isExtensionFunction() {
        if (!(functionDeclaration instanceof JetNamedFunction)) {
            return false;
        }

        FunctionDescriptor functionDescriptor = getFunctionDescriptor(context().bindingContext(), functionDeclaration);
        return DescriptorUtils.isExtensionFunction(functionDescriptor);
    }
}
