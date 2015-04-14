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

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsVars;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.reference.CallExpressionTranslator;
import org.jetbrains.kotlin.psi.JetMultiDeclaration;
import org.jetbrains.kotlin.psi.JetMultiDeclarationEntry;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.js.translate.utils.UtilsPackage.setInlineCallMetadata;

public class MultiDeclarationTranslator extends AbstractTranslator {

    // if multiObjectName was init, initializer must be null
    @NotNull
    public static JsVars translate(
            @NotNull JetMultiDeclaration multiDeclaration,
            @NotNull JsName multiObjectName,
            @Nullable JsExpression initializer,
            @NotNull TranslationContext context
    ) {
        return new MultiDeclarationTranslator(multiDeclaration, multiObjectName, initializer, context).translate();
    }

    @NotNull
    private final JetMultiDeclaration multiDeclaration;
    @NotNull
    private final JsName multiObjectName;
    @Nullable
    private final JsExpression initializer;

    private MultiDeclarationTranslator(
            @NotNull JetMultiDeclaration multiDeclaration,
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
        List<JsVars.JsVar> jsVars = new ArrayList<JsVars.JsVar>();
        if (initializer != null) {
            jsVars.add(new JsVars.JsVar(multiObjectName, initializer));
        }

        JsNameRef multiObjNameRef = multiObjectName.makeRef();
        for (JetMultiDeclarationEntry entry : multiDeclaration.getEntries()) {
            ResolvedCall<FunctionDescriptor> entryInitCall =  context().bindingContext().get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
            assert entryInitCall != null : "Entry init call must be not null";
            JsExpression entryInitializer = CallTranslator.INSTANCE$.translate(context(), entryInitCall, multiObjNameRef);
            FunctionDescriptor candidateDescriptor = entryInitCall.getCandidateDescriptor();
            if (CallExpressionTranslator.shouldBeInlined(candidateDescriptor)) {
                setInlineCallMetadata(entryInitializer, entry, entryInitCall, context());
            }
            VariableDescriptor descriptor = BindingContextUtils.getNotNull( context().bindingContext(), BindingContext.VARIABLE, entry);
            JsName name =  context().getNameForDescriptor(descriptor);
            jsVars.add(new JsVars.JsVar(name, entryInitializer));
        }
        return new JsVars(jsVars, true);
    }
}
