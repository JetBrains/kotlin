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

package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.reflect.ReflectionTypes;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.k2js.translate.context.ContextPackage.getNameForCapturedDescriptor;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForElement;

/**
 * All the info about the state of the translation process.
 */
public class TranslationContext {
    @NotNull
    private final DynamicContext dynamicContext;
    @NotNull
    private final StaticContext staticContext;
    @NotNull
    private final AliasingContext aliasingContext;
    @Nullable
    private final UsageTracker usageTracker;
    @Nullable
    private final TranslationContext parent;
    @Nullable
    private final DefinitionPlace definitionPlace;

    @NotNull
    public static TranslationContext rootContext(@NotNull StaticContext staticContext, JsFunction rootFunction) {
        DynamicContext rootDynamicContext = DynamicContext.rootContext(rootFunction.getScope(), rootFunction.getBody());
        AliasingContext rootAliasingContext = AliasingContext.getCleanContext();
        return new TranslationContext(null, staticContext, rootDynamicContext, rootAliasingContext, null, null);
    }

    private final Map<JsExpression, TemporaryConstVariable> expressionToTempConstVariableCache = new HashMap<JsExpression, TemporaryConstVariable>();

    private TranslationContext(
            @Nullable TranslationContext parent,
            @NotNull StaticContext staticContext,
            @NotNull DynamicContext dynamicContext,
            @NotNull AliasingContext aliasingContext,
            @Nullable UsageTracker usageTracker,
            @Nullable DefinitionPlace definitionPlace
    ) {
        this.parent = parent;
        this.dynamicContext = dynamicContext;
        this.staticContext = staticContext;
        this.aliasingContext = aliasingContext;
        this.usageTracker = usageTracker;
        this.definitionPlace = definitionPlace;
    }

    @Nullable
    public UsageTracker usageTracker() {
        return usageTracker;
    }

    public DynamicContext dynamicContext() {
        return dynamicContext;
    }

    @NotNull
    public TranslationContext contextWithScope(@NotNull JsFunction fun) {
        return this.newFunctionBody(fun, aliasingContext);
    }

    @NotNull
    public TranslationContext newFunctionBody(@NotNull JsFunction fun, @Nullable AliasingContext aliasingContext) {
        DynamicContext dynamicContext = DynamicContext.newContext(fun.getScope(), fun.getBody());
        if (aliasingContext == null) {
            aliasingContext = this.aliasingContext.inner();
        }

        return new TranslationContext(this, this.staticContext, dynamicContext, aliasingContext, this.usageTracker, null);
    }

    @NotNull
    public TranslationContext newFunctionBodyWithUsageTracker(@NotNull JsFunction fun, @NotNull MemberDescriptor descriptor) {
        DynamicContext dynamicContext = DynamicContext.newContext(fun.getScope(), fun.getBody());
        UsageTracker usageTracker = new UsageTracker(this.usageTracker, descriptor, fun.getScope());
        return new TranslationContext(this, this.staticContext, dynamicContext, this.aliasingContext.inner(), usageTracker, this.definitionPlace);
    }

    @NotNull
    public TranslationContext innerBlock(@NotNull JsBlock block) {
        return new TranslationContext(this, staticContext, dynamicContext.innerBlock(block), aliasingContext, usageTracker, null);
    }

    @NotNull
    public TranslationContext newDeclaration(@NotNull DeclarationDescriptor descriptor, @Nullable DefinitionPlace place) {
        DynamicContext dynamicContext = DynamicContext.newContext(getScopeForDescriptor(descriptor), getBlockForDescriptor(descriptor));
        return new TranslationContext(this, staticContext, dynamicContext, aliasingContext, usageTracker, place);
    }

    @NotNull
    private TranslationContext innerWithAliasingContext(AliasingContext aliasingContext) {
        return new TranslationContext(this, this.staticContext, this.dynamicContext, aliasingContext, this.usageTracker, null);
    }

    @NotNull
    public TranslationContext innerContextWithAliased(@NotNull DeclarationDescriptor correspondingDescriptor, @NotNull JsExpression alias) {
        return this.innerWithAliasingContext(aliasingContext.inner(correspondingDescriptor, alias));
    }

    @NotNull
    public TranslationContext innerContextWithAliasesForExpressions(@NotNull Map<JetExpression, JsExpression> aliases) {
        return this.innerWithAliasingContext(aliasingContext.withExpressionsAliased(aliases));
    }

    @NotNull
    public TranslationContext innerContextWithDescriptorsAliased(@NotNull Map<DeclarationDescriptor, JsExpression> aliases) {
        return this.innerWithAliasingContext(aliasingContext.withDescriptorsAliased(aliases));
    }

    @NotNull
    public JsBlock getBlockForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableDescriptor) {
            return getFunctionObject((CallableDescriptor) descriptor).getBody();
        }
        else {
            return new JsBlock();
        }
    }

    @NotNull
    public BindingContext bindingContext() {
        return staticContext.getBindingContext();
    }

    @NotNull
    public JsScope getScopeForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.getScopeForDescriptor(descriptor);
    }

    @NotNull
    public JsName getNameForElement(@NotNull PsiElement element) {
        DeclarationDescriptor descriptor = getDescriptorForElement(bindingContext(), element);
        return getNameForDescriptor(descriptor);
    }

    @NotNull
    public JsName getNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.getNameForDescriptor(descriptor);
    }

    @NotNull
    public JsName getNameForPackage(@NotNull FqName fqName) {
        return staticContext.getNameForPackage(fqName);
    }

    @NotNull
    public JsName declarePropertyOrPropertyAccessorName(@NotNull DeclarationDescriptor descriptor, @NotNull String name, boolean fresh) {
        return staticContext.declarePropertyOrPropertyAccessorName(descriptor, name, fresh);
    }

    @NotNull
    public JsNameRef getQualifiedReference(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.getQualifiedReference(descriptor);
    }

    @NotNull
    public JsNameRef getQualifiedReference(@NotNull FqName packageFqName) {
        return staticContext.getQualifiedReference(packageFqName);
    }

    @Nullable
    public JsNameRef getQualifierForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.getQualifierForDescriptor(descriptor);
    }

    @NotNull
    public TemporaryVariable declareTemporary(@Nullable JsExpression initExpression) {
        return dynamicContext.declareTemporary(initExpression);
    }

    @NotNull
    public TemporaryConstVariable getOrDeclareTemporaryConstVariable(@NotNull JsExpression expression) {
        TemporaryConstVariable tempVar = expressionToTempConstVariableCache.get(expression);

        if (tempVar == null) {
            TemporaryVariable tmpVar = declareTemporary(expression);

            tempVar = new TemporaryConstVariable(tmpVar.name(), tmpVar.assignmentExpression());

            expressionToTempConstVariableCache.put(expression, tempVar);
            expressionToTempConstVariableCache.put(tmpVar.assignmentExpression(), tempVar);
        }

        return tempVar;
    }

    public void associateExpressionToLazyValue(JsExpression expression, TemporaryConstVariable temporaryConstVariable) {
        assert expression == temporaryConstVariable.assignmentExpression();
        expressionToTempConstVariableCache.put(expression, temporaryConstVariable);
    }

    @NotNull
    public Namer namer() {
        return staticContext.getNamer();
    }

    @NotNull
    public Intrinsics intrinsics() {
        return staticContext.getIntrinsics();
    }

    @NotNull
    public ReflectionTypes getReflectionTypes() {
        return staticContext.getReflectionTypes();
    }

    @NotNull
    public JsProgram program() {
        return staticContext.getProgram();
    }

    @NotNull
    public JsScope scope() {
        return dynamicContext.getScope();
    }

    @NotNull
    public AliasingContext aliasingContext() {
        return aliasingContext;
    }

    @NotNull
    public JsFunction getFunctionObject(@NotNull CallableDescriptor descriptor) {
        return staticContext.getFunctionWithScope(descriptor);
    }

    public void addStatementToCurrentBlock(@NotNull JsStatement statement) {
        dynamicContext.jsBlock().getStatements().add(statement);
    }

    @Nullable
    public JsExpression getAliasForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        JsNameRef nameRef = captureIfNeedAndGetCapturedName(descriptor);
        if (nameRef != null) {
            return nameRef;
        }

        return aliasingContext.getAliasForDescriptor(descriptor);
    }

    @NotNull
    public JsExpression getThisObject(@NotNull ReceiverParameterDescriptor descriptor) {
        JsExpression alias = getAliasForDescriptor(descriptor);
        return alias == null ? JsLiteral.THIS : alias;
    }

    @NotNull
    private DefinitionPlace getDefinitionPlace() {
        if (definitionPlace != null) return definitionPlace;
        if (parent != null) return parent.getDefinitionPlace();

        throw new AssertionError("Can not find definition place from rootContext(definitionPlace and parent is null)");
    }

    @NotNull
    public JsNameRef define(DeclarationDescriptor descriptor, JsExpression expression) {
        String suggestedName = TranslationUtils.getSuggestedNameForInnerDeclaration(this, descriptor);
        return getDefinitionPlace().define(suggestedName, expression);
    }

    @Nullable
    private JsNameRef captureIfNeedAndGetCapturedName(DeclarationDescriptor descriptor) {
        if (usageTracker != null && descriptor instanceof CallableDescriptor) {
            CallableDescriptor callableDescriptor = (CallableDescriptor) descriptor;

            usageTracker.used(callableDescriptor);

            JsName name = getNameForCapturedDescriptor(usageTracker, callableDescriptor);
            if (name != null) return name.makeRef();
        }

        return null;
    }
}
