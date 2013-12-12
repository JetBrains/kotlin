/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetClassBody;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.k2js.translate.LabelGenerator;
import org.jetbrains.k2js.translate.context.*;
import org.jetbrains.k2js.translate.declaration.ClassTranslator;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptor;
import static org.jetbrains.k2js.translate.utils.FunctionBodyTranslator.translateFunctionBody;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getExpectedReceiverDescriptor;

public class LiteralFunctionTranslator extends AbstractTranslator {
    private static final LabelGenerator FUNCTION_NAME_GENERATOR = new LabelGenerator('f');

    private LiteralFunctionTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @NotNull
    public static JsExpression translate(@NotNull JetDeclarationWithBody declaration, @NotNull TranslationContext outerContext) {
        FunctionDescriptor descriptor = getFunctionDescriptor(outerContext.bindingContext(), declaration);

        DeclarationDescriptor receiverDescriptor = getExpectedReceiverDescriptor(descriptor);
        JsFunction fun = new JsFunction(outerContext.scope(), new JsBlock());

        AliasingContext aliasingContext;
        JsName receiverName;
        if (receiverDescriptor == null) {
            receiverName = null;
            aliasingContext = null;
        }
        else {
            receiverName = fun.getScope().declareName(Namer.getReceiverParameterName());
            aliasingContext = outerContext.aliasingContext().inner(receiverDescriptor, receiverName.makeRef());
        }

        boolean asInner;
        ClassDescriptor outerClass;
        if (descriptor.getContainingDeclaration() instanceof ConstructorDescriptor) {
            // KT-2388
            asInner = true;
            fun.setName(fun.getScope().declareName(Namer.CALLEE_NAME));
            outerClass = (ClassDescriptor) descriptor.getContainingDeclaration().getContainingDeclaration();
            assert outerClass != null;

            if (receiverDescriptor == null) {
                aliasingContext = outerContext.aliasingContext().notShareableThisAliased(outerClass, new JsNameRef("o", fun.getName().makeRef()));
            }
        }
        else {
            outerClass = null;
            asInner = DescriptorUtils.isTopLevelDeclaration(descriptor);
        }

        UsageTracker funTracker = new UsageTracker(descriptor, outerContext.usageTracker(), outerClass);
        TranslationContext funContext = outerContext.newFunctionBody(fun, aliasingContext, funTracker);

        fun.getBody().getStatements().addAll(translateFunctionBody(descriptor, declaration, funContext).getStatements());

        if (asInner) {
            addRegularParameters(descriptor, fun, funContext, receiverName);
            if (outerClass != null) {
                UsageTracker usageTracker = funContext.usageTracker();
                assert usageTracker != null;
                if (usageTracker.isUsed()) {
                    return new JsInvocation(outerContext.namer().kotlin("assignOwner"), fun, JsLiteral.THIS);
                }
                else {
                    fun.setName(null);
                }
            }

            return fun;
        }

        InnerFunctionTranslator translator = new InnerFunctionTranslator(descriptor, funContext, fun);

        JsExpression result = translator.translate(outerContext.define(FUNCTION_NAME_GENERATOR.generate(), fun), outerContext);
        addRegularParameters(descriptor, fun, funContext, receiverName);
        return result;
    }

    private static void addRegularParameters(
            @NotNull FunctionDescriptor descriptor,
            @NotNull JsFunction fun,
            @NotNull TranslationContext funContext,
            @Nullable JsName receiverName
    ) {
        if (receiverName != null) {
            fun.getParameters().add(new JsParameter(receiverName));
        }
        FunctionTranslator.addParameters(fun.getParameters(), descriptor, funContext);
    }


    // TODO: Probably should be moved to other place
    @NotNull
    public static JsExpression translate(
            @NotNull ClassDescriptor outerClass,
            @NotNull TranslationContext outerClassContext,
            @NotNull JetClassOrObject declaration,
            @NotNull ClassDescriptor descriptor,
            @NotNull ClassTranslator classTranslator
    ) {
        JsFunction fun = new JsFunction(outerClassContext.scope(), new JsBlock());
        JsNameRef outerClassRef = fun.getScope().declareName(Namer.OUTER_CLASS_NAME).makeRef();
        UsageTracker usageTracker = new UsageTracker(descriptor, outerClassContext.usageTracker(), outerClass);
        AliasingContext aliasingContext = outerClassContext.aliasingContext().inner(outerClass, outerClassRef);
        TranslationContext funContext = outerClassContext.newFunctionBody(fun, aliasingContext, usageTracker);

        fun.getBody().getStatements().add(new JsReturn(classTranslator.translate(funContext)));

        JetClassBody body = declaration.getBody();
        assert body != null;

        JsNameRef define = funContext.define(FUNCTION_NAME_GENERATOR.generate(), fun);
        return new InnerObjectTranslator(funContext, fun).translate(define, usageTracker.isUsed() ? outerClassRef : null);
    }
}
