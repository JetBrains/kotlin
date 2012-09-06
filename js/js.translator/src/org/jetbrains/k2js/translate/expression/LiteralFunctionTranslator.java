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
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Trinity;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetClassBody;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;
import org.jetbrains.k2js.translate.LabelGenerator;
import org.jetbrains.k2js.translate.context.AliasingContext;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.context.UsageTracker;
import org.jetbrains.k2js.translate.declaration.ClassTranslator;
import org.jetbrains.k2js.translate.initializer.InitializerUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.FunctionBodyTranslator.translateFunctionBody;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getExpectedReceiverDescriptor;

public class LiteralFunctionTranslator {
    private TranslationContext rootContext;

    private final Stack<NotNullLazyValue<Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression>>> definitionPlaces =
            new Stack<NotNullLazyValue<Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression>>>();
    private NotNullLazyValue<Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression>> definitionPlace;

    public void setRootContext(@NotNull TranslationContext rootContext) {
        assert this.rootContext == null;
        this.rootContext = rootContext;
    }

    public static Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression> createPlace(@NotNull List<JsPropertyInitializer> list,
            @NotNull JsExpression reference) {
        return Trinity.create(list, new LabelGenerator('f'), reference);
    }

    public void setDefinitionPlace(@Nullable NotNullLazyValue<Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression>> place) {
        if (place == null) {
            definitionPlaces.pop();
            definitionPlace = definitionPlaces.isEmpty() ? null : definitionPlaces.peek();
        }
        else {
            definitionPlaces.push(place);
            definitionPlace = place;
        }
    }

    public JsExpression translateFunction(@NotNull JetDeclarationWithBody declaration, @NotNull FunctionDescriptor descriptor, @NotNull TranslationContext outerContext) {
        JsFunction fun = createFunction();
        TranslationContext funContext;
        boolean asInner;
        ClassDescriptor outerClass;
        AliasingContext aliasingContext = rootContext.aliasingContext();
        DeclarationDescriptor receiverDescriptor = getExpectedReceiverDescriptor(descriptor);
        JsName receiverName;
        if (receiverDescriptor == null) {
            receiverName = null;
        }
        else {
            receiverName = fun.getScope().declareName(Namer.getReceiverParameterName());
            aliasingContext = aliasingContext.inner(receiverDescriptor, receiverName.makeRef());
        }

        if (descriptor.getContainingDeclaration() instanceof ConstructorDescriptor) {
            // KT-2388
            asInner = true;
            fun.setName(fun.getScope().declareName(Namer.CALLEE_NAME));
            outerClass = (ClassDescriptor) descriptor.getContainingDeclaration().getContainingDeclaration();
            assert outerClass != null;

            if (receiverDescriptor == null) {
                aliasingContext = aliasingContext.inner(outerClass, new JsNameRef("o", fun.getName().makeRef()));
            }

            funContext = rootContext.contextWithScope(fun, aliasingContext, new UsageTracker(descriptor, outerContext.usageTracker(),
                                                                                             outerClass
            ));
        }
        else {
            outerClass = null;
            asInner = descriptor.getContainingDeclaration() instanceof NamespaceDescriptor;

            funContext = rootContext.contextWithScope(fun, aliasingContext, new UsageTracker(descriptor, outerContext.usageTracker(), null));
        }

        fun.getBody().getStatements().addAll(translateFunctionBody(descriptor, declaration, funContext).getStatements());

        InnerFunctionTranslator translator = null;
        if (!asInner) {
            translator = new InnerFunctionTranslator(descriptor, funContext, fun);
        }

        if (asInner) {
            addRegularParameters(descriptor, fun, funContext, receiverName);
            if (outerClass != null) {
                if (funContext.usageTracker().isUsed()) {
                    return new JsInvocation(rootContext.namer().kotlin("assignOwner"), fun, JsLiteral.THIS);
                }
                else {
                    fun.setName(null);
                }
            }

            return fun;
        }

        JsExpression result = translator.translate(createReference(fun), outerContext);
        addRegularParameters(descriptor, fun, funContext, receiverName);
        return result;
    }

    private JsNameRef createReference(JsFunction fun) {
        Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression> place = definitionPlace.getValue();
        JsNameRef nameRef = new JsNameRef(place.second.generate(), place.third);
        place.first.add(new JsPropertyInitializer(nameRef, InitializerUtils.toDataDescriptor(fun, rootContext)));
        return nameRef;
    }

    private static void addRegularParameters(FunctionDescriptor descriptor,
            JsFunction fun,
            TranslationContext funContext,
            JsName receiverName) {
        if (receiverName != null) {
            fun.getParameters().add(new JsParameter(receiverName));
        }
        FunctionTranslator.addParameters(fun.getParameters(), descriptor, funContext);
    }

    private JsFunction createFunction() {
        return new JsFunction(rootContext.scope(), new JsBlock());
    }

    public JsExpression translate(@NotNull ClassDescriptor outerClass,
            @NotNull JetClassOrObject declaration,
            @NotNull ClassDescriptor descriptor,
            @NotNull ClassTranslator classTranslator) {
        JsFunction fun = createFunction();
        JsNameRef outerClassRef = fun.getScope().declareName("$this").makeRef();
        TranslationContext funContext = rootContext
                .contextWithScope(fun, rootContext.aliasingContext().inner(outerClass, outerClassRef), new UsageTracker(descriptor, null,
                                                                                                                        outerClass));

        fun.getBody().getStatements().add(new JsReturn(classTranslator.translate(funContext)));
        JetClassBody body = declaration.getBody();
        assert body != null;
        InnerObjectTranslator translator = new InnerObjectTranslator(funContext, fun);
        return translator.translate(createReference(fun), funContext.usageTracker().isUsed() ? outerClassRef : null);
    }
}

