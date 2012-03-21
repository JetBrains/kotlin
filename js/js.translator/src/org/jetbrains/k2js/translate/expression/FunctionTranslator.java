/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.translate.expression;


import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.reference.ReferenceTranslator;
import org.jetbrains.k2js.translate.utils.DescriptorUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;
import org.jetbrains.k2js.translate.utils.closure.ClosureContext;
import org.jetbrains.k2js.translate.utils.closure.ClosureUtils;
import org.jetbrains.k2js.translate.utils.mutator.Mutator;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptor;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.*;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.convertToBlock;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.setParameters;
import static org.jetbrains.k2js.translate.utils.mutator.LastExpressionMutator.mutateLastExpression;


/**
 * @author Pavel Talanov
 */

//TODO: class has convoluted code. REFACTOR before changing
public final class FunctionTranslator extends AbstractTranslator {

    @NotNull
    public static FunctionTranslator newInstance(@NotNull JetDeclarationWithBody function,
                                                 @NotNull TranslationContext context) {
        return new FunctionTranslator(function, context);
    }

    @Nullable
    private TemporaryVariable aliasForContaingClassThis = null;
    @NotNull
    private final JetDeclarationWithBody functionDeclaration;
    @Nullable
    private JsName extensionFunctionReceiverName = null;
    @NotNull
    private final JsFunction functionObject;
    @NotNull
    private final FunctionDescriptor descriptor;
    // function body needs to be explicitly created here to include it in the context
    @NotNull
    private final JsBlock functionBody;

    private FunctionTranslator(@NotNull JetDeclarationWithBody functionDeclaration,
                               @NotNull TranslationContext context) {
        super(context);
        this.descriptor = getFunctionDescriptor(context.bindingContext(), functionDeclaration);
        this.functionDeclaration = functionDeclaration;
        this.functionObject = context().getFunctionObject(descriptor);
        this.functionBody = functionObject.getBody();
    }

    @NotNull
    private TranslationContext getFunctionBodyContext() {
        if (isLiteral()) {
            return getFunctionBodyContextForLiteral();
        }
        if (isExtensionFunction()) {
            return getFunctionBodyContextForExtensionFunction();
        }
        return getContextWithFunctionBodyBlock();
    }

    @NotNull
    private TranslationContext getFunctionBodyContextForLiteral() {
        ClassDescriptor containingClass = getContainingClass(descriptor);
        if (containingClass == null) {
            return getContextWithFunctionBodyBlock();
        }
        JsExpression thisQualifier = TranslationUtils.getThisObject(context(), containingClass);
        aliasForContaingClassThis = context().declareTemporary(thisQualifier);
        return getContextWithFunctionBodyBlock().innerContextWithThisAliased(containingClass, aliasForContaingClassThis.name());
    }

    @NotNull
    private TranslationContext getFunctionBodyContextForExtensionFunction() {
        TranslationContext contextWithFunctionBodyBlock = getContextWithFunctionBodyBlock();
        extensionFunctionReceiverName = contextWithFunctionBodyBlock.jsScope().declareName(Namer.getReceiverParameterName());
        DeclarationDescriptor expectedReceiverDescriptor = getExpectedReceiverDescriptor(descriptor);
        assert expectedReceiverDescriptor != null;
        return contextWithFunctionBodyBlock.innerContextWithThisAliased(expectedReceiverDescriptor, extensionFunctionReceiverName);
    }

    @NotNull
    private TranslationContext getContextWithFunctionBodyBlock() {
        return context().newDeclaration(functionDeclaration).innerBlock(functionBody);
    }


    @NotNull
    public JsFunction translateAsLocalFunction() {
        JsName functionName = context().getNameForElement(functionDeclaration);
        generateFunctionObject(getFunctionBodyContext());
        functionObject.setName(functionName);
        return functionObject;
    }

    @NotNull
    public JsPropertyInitializer translateAsMethod() {
        JsName functionName = context().getNameForElement(functionDeclaration);
        generateFunctionObject(getFunctionBodyContext());
        return new JsPropertyInitializer(functionName.makeRef(), functionObject);
    }

    @NotNull
    public JsExpression translateAsLiteral() {
        return mayBeWrapInClosureCaptureExpression(doTranslateAsLiteral());
    }

    @NotNull
    private JsExpression doTranslateAsLiteral() {
        assert getExpectedThisDescriptor(descriptor) == null;
        generateFunctionObject(getFunctionBodyContext());
        boolean shouldAliasThisObject = shouldAliasThisObject();
        if (!shouldAliasThisObject) {
            return functionObject;
        }
        assert aliasForContaingClassThis != null;
        return AstUtil.newSequence(aliasForContaingClassThis.assignmentExpression(), functionObject);
    }

    private boolean shouldAliasThisObject() {
        if (!isLiteral()) {
            return false;
        }
        ClassDescriptor containingClass = getContainingClass(descriptor);
        return containingClass != null;
    }

    @NotNull
    private JsExpression mayBeWrapInClosureCaptureExpression(@NotNull JsExpression wrappedExpression) {
        ClosureContext closureContext = ClosureUtils.captureClosure(context(), (JetElement)functionDeclaration);
        if (closureContext.getDescriptors().isEmpty()) {
            return wrappedExpression;
        }
        return wrapInClosureCaptureExpression(wrappedExpression, closureContext);
    }

    @NotNull
    private JsExpression wrapInClosureCaptureExpression(@NotNull JsExpression wrappedExpression,
                                                        @NotNull ClosureContext closureContext) {
        JsFunction dummyFunction = new JsFunction(context().jsScope());
        JsInvocation dummyFunctionInvocation = AstUtil.newInvocation(dummyFunction);
        for (VariableDescriptor variableDescriptor : closureContext.getDescriptors()) {
            dummyFunction.getParameters().add(new JsParameter(context().getNameForDescriptor(variableDescriptor)));
            dummyFunctionInvocation.getArguments().add(ReferenceTranslator.translateAsLocalNameReference(variableDescriptor, context()));
        }
        dummyFunction.setBody(AstUtil.newBlock(new JsReturn(wrappedExpression)));
        return dummyFunctionInvocation;
    }

    private void generateFunctionObject(@NotNull TranslationContext context) {
        setParameters(functionObject, translateParameters());
        translateBody(context);
    }

    private void translateBody(@NotNull TranslationContext context) {
        JetExpression jetBodyExpression = functionDeclaration.getBodyExpression();
        if (jetBodyExpression == null) {
            assert descriptor.getModality().equals(Modality.ABSTRACT);
            return;
        }
        JsNode realBody = Translation.translateExpression(jetBodyExpression, context);
        functionBody.getStatements().add(wrapWithReturnIfNeeded(realBody, mustAddReturnToGeneratedFunctionBody()));
    }

    private boolean mustAddReturnToGeneratedFunctionBody() {
        JetType functionReturnType = descriptor.getReturnType();
        assert functionReturnType != null : "Function return typed type must be resolved.";
        return (!functionDeclaration.hasBlockBody()) && (!JetStandardClasses.isUnit(functionReturnType));
    }

    @NotNull
    private static JsBlock wrapWithReturnIfNeeded(@NotNull JsNode body, boolean needsReturn) {
        if (!needsReturn) {
            return convertToBlock(body);
        }
        return convertToBlock(lastExpressionReturned(body));
    }

    private static JsNode lastExpressionReturned(@NotNull JsNode body) {
        return mutateLastExpression(body, new Mutator() {
            @Override
            @NotNull
            public JsNode mutate(@NotNull JsNode node) {
                if (!(node instanceof JsExpression)) {
                    return node;
                }
                return new JsReturn((JsExpression)node);
            }
        });
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
        return context().getNameForDescriptor(valueParameter);
    }

    private void mayBeAddThisParameterForExtensionFunction(@NotNull List<JsParameter> jsParameters) {
        if (isExtensionFunction()) {
            assert extensionFunctionReceiverName != null;
            jsParameters.add(new JsParameter(extensionFunctionReceiverName));
        }
    }

    private boolean isExtensionFunction() {
        return DescriptorUtils.isExtensionFunction(descriptor) && !isLiteral();
    }

    private boolean isLiteral() {
        return functionDeclaration instanceof JetFunctionLiteralExpression;
    }
}
