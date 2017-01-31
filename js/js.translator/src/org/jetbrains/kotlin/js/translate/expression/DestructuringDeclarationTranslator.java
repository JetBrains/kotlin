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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsName;
import org.jetbrains.kotlin.js.backend.ast.JsNameRef;
import org.jetbrains.kotlin.js.backend.ast.JsVars;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.reference.CallExpressionTranslator;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration;
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.js.translate.context.Namer.getCapturedVarAccessor;
import static org.jetbrains.kotlin.js.translate.utils.InlineUtils.setInlineCallMetadata;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.isVarCapturedInClosure;

public class DestructuringDeclarationTranslator extends AbstractTranslator {

    // if multiObjectName was init, initializer must be null
    @NotNull
    public static JsVars translate(
            @NotNull KtDestructuringDeclaration multiDeclaration,
            @NotNull JsName multiObjectName,
            @Nullable JsExpression initializer,
            @NotNull TranslationContext context
    ) {
        return new DestructuringDeclarationTranslator(multiDeclaration, multiObjectName, initializer, context).translate();
    }

    @NotNull
    private final KtDestructuringDeclaration multiDeclaration;
    @NotNull
    private final JsName multiObjectName;
    @Nullable
    private final JsExpression initializer;

    private DestructuringDeclarationTranslator(
            @NotNull KtDestructuringDeclaration multiDeclaration,
            @NotNull JsName multiObjectName,
            @Nullable JsExpression initializer,
            @NotNull TranslationContext context
    ) {
        super(context);
        this.multiDeclaration = multiDeclaration;
        this.multiObjectName = multiObjectName;
        this.initializer = initializer;
    }

    private JsVars translate() {
        if (initializer != null) {
            context().getCurrentBlock().getStatements().add(JsAstUtils.newVar(multiObjectName, initializer));
        }

        List<JsVars.JsVar> jsVars = new ArrayList<JsVars.JsVar>();
        JsNameRef multiObjNameRef = multiObjectName.makeRef();
        for (KtDestructuringDeclarationEntry entry : multiDeclaration.getEntries()) {
            VariableDescriptor descriptor = BindingContextUtils.getNotNull(context().bindingContext(), BindingContext.VARIABLE, entry);
            // Do not call `componentX` for destructuring entry called _
            if (descriptor.getName().isSpecial()) continue;

            ResolvedCall<FunctionDescriptor> entryInitCall = context().bindingContext().get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
            assert entryInitCall != null : "Entry init call must be not null";
            JsExpression entryInitializer = CallTranslator.translate(context(), entryInitCall, multiObjNameRef);
            FunctionDescriptor candidateDescriptor = entryInitCall.getCandidateDescriptor();
            if (CallExpressionTranslator.shouldBeInlined(candidateDescriptor, context())) {
                setInlineCallMetadata(entryInitializer, entry, entryInitCall, context());
            }

            KotlinType returnType = candidateDescriptor.getReturnType();
            if (returnType != null && KotlinBuiltIns.isCharOrNullableChar(returnType) && !KotlinBuiltIns.isCharOrNullableChar(descriptor.getType())) {
                entryInitializer = JsAstUtils.charToBoxedChar(entryInitializer);
            }

            JsName name = context().getNameForDescriptor(descriptor);
            if (isVarCapturedInClosure(context().bindingContext(), descriptor)) {
                JsNameRef alias = getCapturedVarAccessor(name.makeRef());
                entryInitializer = JsAstUtils.wrapValue(alias, entryInitializer);
            }

            jsVars.add(new JsVars.JsVar(name, entryInitializer));
        }
        return new JsVars(jsVars, true);
    }
}
