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

package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.Named;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.translate.expression.LiteralFunctionTranslator;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;

import java.util.Map;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForElement;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getExpectedReceiverDescriptor;

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

    @NotNull
    public static TranslationContext rootContext(@NotNull StaticContext staticContext, JsFunction rootFunction) {
        DynamicContext rootDynamicContext =
                DynamicContext.rootContext(rootFunction.getScope(), rootFunction.getBody());
        AliasingContext rootAliasingContext = AliasingContext.getCleanContext();
        return new TranslationContext(staticContext, rootDynamicContext, rootAliasingContext, null);
    }

    public boolean isEcma5() {
        return staticContext.isEcma5();
    }

    private TranslationContext(@NotNull StaticContext staticContext,
            @NotNull DynamicContext dynamicContext,
            @NotNull AliasingContext aliasingContext,
            @Nullable UsageTracker usageTracker) {
        this.dynamicContext = dynamicContext;
        this.staticContext = staticContext;
        this.aliasingContext = aliasingContext;
        this.usageTracker = usageTracker;
    }

    private TranslationContext(@NotNull TranslationContext parent, @NotNull AliasingContext aliasingContext) {
        dynamicContext = parent.dynamicContext;
        staticContext = parent.staticContext;
        this.aliasingContext = aliasingContext;
        usageTracker = parent.usageTracker;
    }

    public UsageTracker usageTracker() {
        return usageTracker;
    }

    public DynamicContext dynamicContext() {
        return dynamicContext;
    }

    @NotNull
    public TranslationContext contextWithScope(@NotNull JsFunction fun) {
        return contextWithScope(fun, aliasingContext, usageTracker);
    }

    @NotNull
    private TranslationContext contextWithScope(@NotNull JsScope newScope,
            @NotNull JsBlock block,
            @NotNull AliasingContext aliasingContext,
            @Nullable UsageTracker usageTracker) {
        return new TranslationContext(staticContext, DynamicContext.newContext(newScope, block), aliasingContext, usageTracker);
    }

    @NotNull
    public TranslationContext contextWithScope(@NotNull JsFunction fun, @NotNull AliasingContext aliasingContext, @Nullable UsageTracker usageTracker) {
        return contextWithScope(fun.getScope(), fun.getBody(), aliasingContext, usageTracker);
    }

    @NotNull
    public TranslationContext innerBlock(@NotNull JsBlock block) {
        return new TranslationContext(staticContext, dynamicContext.innerBlock(block), aliasingContext, usageTracker);
    }

    @NotNull
    public TranslationContext newDeclaration(@NotNull DeclarationDescriptor descriptor) {
        return contextWithScope(getScopeForDescriptor(descriptor), getBlockForDescriptor(descriptor), aliasingContext, usageTracker);
    }

    @NotNull
    public TranslationContext innerContextWithThisAliased(@NotNull DeclarationDescriptor correspondingDescriptor, @NotNull JsName alias) {
        return new TranslationContext(this, aliasingContext.inner(correspondingDescriptor, alias.makeRef()));
    }

    @NotNull
    public TranslationContext innerContextWithAliasesForExpressions(@NotNull Map<JetExpression, JsName> aliases) {
        return new TranslationContext(this, aliasingContext.withAliasesForExpressions(aliases));
    }

    @NotNull
    public TranslationContext innerContextWithDescriptorsAliased(@NotNull Map<DeclarationDescriptor, JsExpression> aliases) {
        return new TranslationContext(this, aliasingContext.withDescriptorsAliased(aliases));
    }

    @NotNull
    public JsBlock getBlockForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableDescriptor) {
            return getFunctionObject((CallableDescriptor)descriptor).getBody();
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
    public JsStringLiteral nameToLiteral(@NotNull Named named) {
        return program().getStringLiteral(named.getName().getName());
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
    public Namer namer() {
        return staticContext.getNamer();
    }

    @NotNull
    public Intrinsics intrinsics() {
        return staticContext.getIntrinsics();
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
    public LiteralFunctionTranslator literalFunctionTranslator() {
        return staticContext.getLiteralFunctionTranslator();
    }

    @NotNull
    public JsFunction getFunctionObject(@NotNull CallableDescriptor descriptor) {
        return staticContext.getFunctionWithScope(descriptor);
    }

    public void addStatementToCurrentBlock(@NotNull JsStatement statement) {
        dynamicContext.jsBlock().getStatements().add(statement);
    }

    public JsExpression getAliasForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (usageTracker != null) {
            usageTracker.triggerUsed(descriptor);
        }
        return aliasingContext.getAliasForDescriptor(descriptor);
    }

    @NotNull
    public JsExpression getThisObject(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor effectiveDescriptor;
        if (descriptor instanceof CallableDescriptor) {
            effectiveDescriptor = getExpectedReceiverDescriptor((CallableDescriptor) descriptor);
            assert effectiveDescriptor != null;
        }
        else {
            effectiveDescriptor = descriptor;
        }

        if (usageTracker != null) {
            usageTracker.triggerUsed(effectiveDescriptor);
        }

        JsExpression alias = aliasingContext.getAliasForDescriptor(effectiveDescriptor);
        return alias == null ? JsLiteral.THIS : alias;
    }
}
