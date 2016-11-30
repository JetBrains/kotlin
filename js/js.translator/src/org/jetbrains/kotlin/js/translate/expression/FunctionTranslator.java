/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.metadata.MetadataProperties;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.translate.context.AliasingContext;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.psi.KtDeclarationWithBody;
import org.jetbrains.kotlin.psi.KtLambdaExpression;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.js.translate.reference.CallExpressionTranslator.shouldBeInlined;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getFunctionDescriptor;
import static org.jetbrains.kotlin.js.translate.utils.ErrorReportingUtils.message;
import static org.jetbrains.kotlin.js.translate.utils.FunctionBodyTranslator.translateFunctionBody;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.setParameters;

public final class FunctionTranslator extends AbstractTranslator {
    @NotNull
    public static FunctionTranslator newInstance(
            @NotNull KtDeclarationWithBody declaration,
            @NotNull TranslationContext context,
            @NotNull JsFunction function
    ) {
        return new FunctionTranslator(declaration, context, function);
    }

    @NotNull
    private TranslationContext functionBodyContext;
    @NotNull
    private final KtDeclarationWithBody functionDeclaration;
    @Nullable
    private JsName extensionFunctionReceiverName;
    @NotNull
    private final JsFunction functionObject;
    @NotNull
    private final FunctionDescriptor descriptor;

    private FunctionTranslator(@NotNull KtDeclarationWithBody functionDeclaration, @NotNull TranslationContext context,
            @NotNull JsFunction function) {
        super(context);
        this.descriptor = getFunctionDescriptor(context.bindingContext(), functionDeclaration);
        this.functionDeclaration = functionDeclaration;
        this.functionObject = function;
        assert this.functionObject.getParameters().isEmpty()
                : message(descriptor, "Function " + functionDeclaration.getText() + " processed for the second time.");
        //NOTE: it's important we compute the context before we start the computation
        this.functionBodyContext = getFunctionBodyContext();
        MetadataProperties.setFunctionDescriptor(functionObject, descriptor);
    }

    @NotNull
    private TranslationContext getFunctionBodyContext() {
        AliasingContext aliasingContext;
        if (isExtensionFunction()) {
            DeclarationDescriptor expectedReceiverDescriptor = descriptor.getExtensionReceiverParameter();
            assert expectedReceiverDescriptor != null;
            extensionFunctionReceiverName = functionObject.getScope().declareName(Namer.getReceiverParameterName());
            //noinspection ConstantConditions
            aliasingContext = context().aliasingContext().inner(expectedReceiverDescriptor, extensionFunctionReceiverName.makeRef());
        }
        else {
            aliasingContext = null;
        }
        return context().newFunctionBody(functionObject, aliasingContext, descriptor);
    }

    @NotNull
    public JsPropertyInitializer translateAsEcma5PropertyDescriptor() {
        generateFunctionObject();
        return TranslationUtils.translateFunctionAsEcma5PropertyDescriptor(functionObject, descriptor, context());
    }

    public void translateAsMethodWithoutMetadata() {
        generateFunctionObject();
    }

    @NotNull
    public JsExpression translateAsMethod() {
        translateAsMethodWithoutMetadata();

        if (shouldBeInlined(descriptor, context()) && DescriptorUtilsKt.isEffectivelyPublicApi(descriptor)) {
            InlineMetadata metadata = InlineMetadata.compose(functionObject, descriptor);
            return metadata.getFunctionWithMetadata();
        }
        else {
            return functionObject;
        }
    }

    private void generateFunctionObject() {
        List<JsParameter> parameters = translateParameters();

        VariableDescriptor continuationDescriptor = functionBodyContext.getContinuationParameterDescriptor();
        if (continuationDescriptor != null) {
            JsParameter jsParameter = new JsParameter(functionBodyContext.getNameForDescriptor(continuationDescriptor));
            parameters.add(jsParameter);
        }

        setParameters(functionObject, parameters);
        translateBody();
    }

    private void translateBody() {
        if (!functionDeclaration.hasBody()) {
            assert descriptor instanceof ConstructorDescriptor || descriptor.getModality().equals(Modality.ABSTRACT);
            return;
        }
        JsBlock body = translateFunctionBody(descriptor, functionDeclaration, functionBodyContext);
        functionObject.getBody().getStatements().addAll(body.getStatements());
    }

    @NotNull
    private List<JsParameter> translateParameters() {
        List<JsParameter> jsParameters = new SmartList<JsParameter>();
        Map<DeclarationDescriptor, JsExpression> aliases = new HashMap<DeclarationDescriptor, JsExpression>();

        for (TypeParameterDescriptor type : getTypeParameters(descriptor)) {
            if (type.isReified()) {
                JsName paramNameForType = context().getNameForDescriptor(type);
                jsParameters.add(new JsParameter(paramNameForType));

                String suggestedName = Namer.isInstanceSuggestedName(type);
                JsName paramName = functionObject.getScope().declareName(suggestedName);
                jsParameters.add(new JsParameter(paramName));
                aliases.put(type, paramName.makeRef());
            }
        }

        functionBodyContext = functionBodyContext.innerContextWithDescriptorsAliased(aliases);

        if (extensionFunctionReceiverName == null && descriptor.getValueParameters().isEmpty()) {
            return jsParameters;
        }

        mayBeAddThisParameterForExtensionFunction(jsParameters);
        addParameters(jsParameters, descriptor, context());

        return jsParameters;
    }

    private static List<TypeParameterDescriptor> getTypeParameters(FunctionDescriptor functionDescriptor) {
        if (functionDescriptor instanceof PropertyAccessorDescriptor) {
            return ((PropertyAccessorDescriptor) functionDescriptor).getCorrespondingProperty().getTypeParameters();
        }
        return functionDescriptor.getTypeParameters();
    }

    public static void addParameters(List<JsParameter> list, FunctionDescriptor descriptor, TranslationContext context) {
        for (ValueParameterDescriptor valueParameter : descriptor.getValueParameters()) {
            JsParameter jsParameter = new JsParameter(context.getNameForDescriptor(valueParameter));
            MetadataProperties.setHasDefaultValue(jsParameter, DescriptorUtilsKt.hasDefaultValue(valueParameter));
            list.add(jsParameter);
        }
    }

    private void mayBeAddThisParameterForExtensionFunction(@NotNull List<JsParameter> jsParameters) {
        if (isExtensionFunction()) {
            assert extensionFunctionReceiverName != null;
            jsParameters.add(new JsParameter(extensionFunctionReceiverName));
        }
    }

    private boolean isExtensionFunction() {
        return DescriptorUtils.isExtension(descriptor) && !(functionDeclaration instanceof KtLambdaExpression);
    }
}
