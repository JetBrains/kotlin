/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.expression;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsName;
import org.jetbrains.kotlin.js.backend.ast.JsNameRef;
import org.jetbrains.kotlin.js.backend.ast.JsVars;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.reference.CallExpressionTranslator;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration;
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.js.translate.context.Namer.getCapturedVarAccessor;
import static org.jetbrains.kotlin.js.translate.utils.InlineUtils.setInlineCallMetadata;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.isBoxedLocalCapturedInClosure;

public class DestructuringDeclarationTranslator extends AbstractTranslator {

    // if multiObjectName was init, initializer must be null
    @NotNull
    public static JsVars translate(
            @NotNull KtDestructuringDeclaration multiDeclaration,
            @NotNull JsExpression multiObjectExpr,
            @NotNull TranslationContext context
    ) {
        return new DestructuringDeclarationTranslator(multiDeclaration, multiObjectExpr, context).translate();
    }

    @NotNull
    private final KtDestructuringDeclaration multiDeclaration;
    @NotNull
    private final JsExpression multiObjectExpr;

    private DestructuringDeclarationTranslator(
            @NotNull KtDestructuringDeclaration multiDeclaration,
            @NotNull JsExpression multiObjectExpr,
            @NotNull TranslationContext context
    ) {
        super(context);
        this.multiDeclaration = multiDeclaration;
        this.multiObjectExpr = multiObjectExpr;
    }

    private JsVars translate() {
        List<JsVars.JsVar> jsVars = new ArrayList<>();
        for (KtDestructuringDeclarationEntry entry : multiDeclaration.getEntries()) {
            VariableDescriptor descriptor = BindingContextUtils.getNotNull(context().bindingContext(), BindingContext.VARIABLE, entry);
            // Do not call `componentX` for destructuring entry called _
            if (descriptor.getName().isSpecial()) continue;

            ResolvedCall<FunctionDescriptor> entryInitCall = context().bindingContext().get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
            assert entryInitCall != null : "Entry init call must be not null";
            JsExpression entryInitializer = CallTranslator.translate(context(), entryInitCall, multiObjectExpr);
            FunctionDescriptor candidateDescriptor = entryInitCall.getCandidateDescriptor();
            if (CallExpressionTranslator.shouldBeInlined(candidateDescriptor, context())) {
                setInlineCallMetadata(entryInitializer, entry, entryInitCall, context());
            }

            entryInitializer = TranslationUtils.coerce(context(), entryInitializer, descriptor.getType());

            JsName name = context().getNameForDescriptor(descriptor);
            if (isBoxedLocalCapturedInClosure(context().bindingContext(), descriptor)) {
                JsNameRef alias = getCapturedVarAccessor(name.makeRef());
                entryInitializer = JsAstUtils.wrapValue(alias, entryInitializer);
            }

            JsVars.JsVar jsVar = new JsVars.JsVar(name, entryInitializer);
            jsVar.setSource(entry);
            jsVars.add(jsVar);
        }
        JsVars result = new JsVars(jsVars, true);
        result.setSource(multiDeclaration);
        return result;
    }
}
