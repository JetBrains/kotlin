package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.DescriptorUtils;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptor;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.*;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.*;


/**
 * @author Pavel Talanov
 */
public final class FunctionTranslator extends AbstractTranslator {

    @NotNull
    public static FunctionTranslator newInstance(@NotNull JetDeclarationWithBody function,
                                                 @NotNull TranslationContext context) {
        return new FunctionTranslator(function, context);
    }

    //TODO: implement more generic and safe way
    @NotNull
    private static final String RECEIVER_PARAMETER_NAME = "receiver";

    @NotNull
    private final JetDeclarationWithBody functionDeclaration;
    @NotNull
    private final JsFunction functionObject;
    @NotNull
    private final TranslationContext functionBodyContext;
    @NotNull
    private final FunctionDescriptor descriptor;
    // function body needs to be explicitly created here to include it in the context
    @NotNull
    private final JsBlock functionBody;

    private FunctionTranslator(@NotNull JetDeclarationWithBody functionDeclaration,
                               @NotNull TranslationContext context) {
        super(context);
        this.functionBody = new JsBlock();
        this.functionDeclaration = functionDeclaration;
        this.functionObject = createFunctionObject();
        this.functionBodyContext = functionBodyContext().innerBlock(functionBody);
        this.descriptor = getFunctionDescriptor(context.bindingContext(), functionDeclaration);
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
        assert getExpectedThisDescriptor(descriptor) == null;
        ClassDescriptor containingClass = getContainingClass(descriptor);
        if (containingClass == null) {
            return generateFunctionObject();
        }
        return generateFunctionObjectWithAliasedThisReference(containingClass);
    }

    @NotNull
    private JsExpression generateFunctionObjectWithAliasedThisReference(@NotNull ClassDescriptor containingClass) {
        TemporaryVariable aliasForThis = newAliasForThis(context(), containingClass);
        JsFunction function = generateFunctionObject();
        removeAliasForThis(context(), containingClass);
        return AstUtil.newSequence(aliasForThis.assignmentExpression(), function);
    }

    @NotNull
    private JsFunction generateFunctionObject() {
        functionObject.setParameters(translateParameters());
        translateBody();
        functionObject.setBody(functionBody);
        restoreContext();
        return functionObject;
    }

    private void restoreContext() {
        if (isExtensionFunction()) {
            DeclarationDescriptor expectedReceiverDescriptor = getExpectedReceiverDescriptor(descriptor);
            assert expectedReceiverDescriptor != null : "Extension functions should always have receiver descriptors.";
            functionBodyContext.aliaser().removeAliasForThis(expectedReceiverDescriptor);
        }
    }

    @NotNull
    private JsFunction createFunctionObject() {
        if (isDeclaration()) {
            return functionWithScope(context().getScopeForElement((JetDeclaration) functionDeclaration));
        }
        if (isLiteral()) {
            //TODO: look into
            return new JsFunction(context().jsScope());
        }
        throw new AssertionError("Unsupported type of functionDeclaration.");
    }

    private void translateBody() {
        JetExpression jetBodyExpression = functionDeclaration.getBodyExpression();
        if (jetBodyExpression == null) {
            assert descriptor.getModality().equals(Modality.ABSTRACT);
            return;
        }
        JsNode realBody = Translation.translateExpression(jetBodyExpression, functionBodyContext);
        functionBody.addStatement(wrapWithReturnIfNeeded(realBody, mustAddReturnToGeneratedFunctionBody()));
    }

    private boolean mustAddReturnToGeneratedFunctionBody() {
        JetType functionReturnType = descriptor.getReturnType();
        assert functionReturnType != null : "Function return typed type must be resolved.";
        return (!functionDeclaration.hasBlockBody()) && (!JetStandardClasses.isUnit(functionReturnType));
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
        JsReturn returnStatement = new JsReturn(AstUtil.extractExpressionFromStatement(lastStatement));
        statements.set(lastIndex, returnStatement);
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
    private List<JsParameter> translateParameters() {
        List<JsParameter> jsParameters = new ArrayList<JsParameter>();
        mayBeAddThisParameterForExtensionFunction(jsParameters);
        for (ValueParameterDescriptor valueParameter : descriptor.getValueParameters()) {
            JsName parameterName = declareParameter(valueParameter);
            jsParameters.add(new JsParameter(parameterName));
        }
        return jsParameters;
    }

    @NotNull
    private JsName declareParameter(@NotNull ValueParameterDescriptor valueParameter) {
        return context().declareLocalVariable(valueParameter);
    }

    private void mayBeAddThisParameterForExtensionFunction(@NotNull List<JsParameter> jsParameters) {
        if (isExtensionFunction()) {
            //TODO: dont do this
            JsName receiver = functionBodyContext.jsScope().declareName(RECEIVER_PARAMETER_NAME);
            DeclarationDescriptor expectedReceiverDescriptor = getExpectedReceiverDescriptor(descriptor);
            assert expectedReceiverDescriptor != null;
            context().aliaser().setAliasForThis(expectedReceiverDescriptor, receiver);
            jsParameters.add(new JsParameter(receiver));
        }
    }

    private boolean isExtensionFunction() {
        return DescriptorUtils.isExtensionFunction(descriptor);
    }

    private boolean isLiteral() {
        return functionDeclaration instanceof JetFunctionLiteralExpression;
    }

    private boolean isDeclaration() {
        return (functionDeclaration instanceof JetNamedFunction) ||
                (functionDeclaration instanceof JetPropertyAccessor);
    }
}
