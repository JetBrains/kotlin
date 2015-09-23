/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.psi.JetDeclarationWithBody;
import org.jetbrains.kotlin.psi.JetFunctionLiteralExpression;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.js.translate.reference.CallExpressionTranslator.shouldBeInlined;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getFunctionDescriptor;
import static org.jetbrains.kotlin.js.translate.utils.ErrorReportingUtils.message;
import static org.jetbrains.kotlin.js.translate.utils.FunctionBodyTranslator.translateFunctionBody;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.setParameters;
import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getTEMP_isEffectivelyPublicApi;

public final class FunctionTranslator extends AbstractTranslator {
    @NotNull
    public static FunctionTranslator newInstance(@NotNull JetDeclarationWithBody function,
            @NotNull TranslationContext context) {
        return new FunctionTranslator(function, context);
    }

    @NotNull
    private TranslationContext functionBodyContext;
    @NotNull
    private final JetDeclarationWithBody functionDeclaration;
    @Nullable
    private JsName extensionFunctionReceiverName;
    @NotNull
    private final JsFunction functionObject;
    @NotNull
    private final FunctionDescriptor descriptor;

    private FunctionTranslator(@NotNull JetDeclarationWithBody functionDeclaration, @NotNull TranslationContext context) {
        super(context);
        this.descriptor = getFunctionDescriptor(context.bindingContext(), functionDeclaration);
        this.functionDeclaration = functionDeclaration;
        this.functionObject = context().getFunctionObject(descriptor);
        assert this.functionObject.getParameters().isEmpty()
                : message(descriptor, "Function " + functionDeclaration.getText() + " processed for the second time.");
        //NOTE: it's important we compute the context before we start the computation
        this.functionBodyContext = getFunctionBodyContext();
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
        return context().newFunctionBody(functionObject, aliasingContext);
    }

    @NotNull
    public JsPropertyInitializer translateAsEcma5PropertyDescriptor() {
        generateFunctionObject();
        return TranslationUtils.translateFunctionAsEcma5PropertyDescriptor(functionObject, descriptor, context());
    }

    @NotNull
    public JsPropertyInitializer translateAsMethod() {
        JsName functionName = context().getNameForDescriptor(descriptor);
        generateFunctionObject();

        if (shouldBeInlined(descriptor) && getTEMP_isEffectivelyPublicApi(descriptor)) {
            InlineMetadata metadata = InlineMetadata.compose(functionObject, descriptor);
            return new JsPropertyInitializer(functionName.makeRef(), metadata.getFunctionWithMetadata());
        }

        return new JsPropertyInitializer(functionName.makeRef(), functionObject);
    }

    private void generateFunctionObject() {
        setParameters(functionObject, translateParameters());
        translateBody();
    }

    private void translateBody() {
        if (!functionDeclaration.hasBody()) {
            assert descriptor instanceof ConstructorDescriptor || descriptor.getModality().equals(Modality.ABSTRACT);
            return;
        }
        functionObject.getBody().getStatements().addAll(translateFunctionBody(descriptor, functionDeclaration, functionBodyContext).getStatements());
    }

    @NotNull
    private List<JsParameter> translateParameters() {
        List<JsParameter> jsParameters = new SmartList<JsParameter>();
        Map<DeclarationDescriptor, JsExpression> aliases = new HashMap<DeclarationDescriptor, JsExpression>();

        for (TypeParameterDescriptor type : descriptor.getTypeParameters()) {
            if (type.isReified()) {
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

    public static void addParameters(List<JsParameter> list, FunctionDescriptor descriptor, TranslationContext context) {
        for (ValueParameterDescriptor valueParameter : descriptor.getValueParameters()) {
            JsParameter jsParameter = new JsParameter(context.getNameForDescriptor(valueParameter));
            MetadataProperties.setHasDefaultValue(jsParameter, DescriptorUtilPackage.hasDefaultValue(valueParameter));
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
        return DescriptorUtils.isExtension(descriptor) && !(functionDeclaration instanceof JetFunctionLiteralExpression);
    }
}
