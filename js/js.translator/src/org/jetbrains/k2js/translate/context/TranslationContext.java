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

    @NotNull
    public static TranslationContext rootContext(@NotNull StaticContext staticContext) {
        JsProgram program = staticContext.getProgram();
        JsBlock globalBlock = program.getGlobalBlock();
        DynamicContext rootDynamicContext = DynamicContext.rootContext(staticContext.getRootScope(), globalBlock);
        AliasingContext rootAliasingContext = AliasingContext.getCleanContext();
        return new TranslationContext(staticContext,
                                      rootDynamicContext, rootAliasingContext);
    }

    public boolean isEcma5() {
        return staticContext.isEcma5();
    }

    private TranslationContext(@NotNull StaticContext staticContext,
            @NotNull DynamicContext dynamicContext,
            @NotNull AliasingContext aliasingContext) {
        this.dynamicContext = dynamicContext;
        this.staticContext = staticContext;
        this.aliasingContext = aliasingContext;
    }

    private TranslationContext(@NotNull TranslationContext parent, @NotNull AliasingContext aliasingContext) {
        dynamicContext = parent.dynamicContext;
        staticContext = parent.staticContext;
        this.aliasingContext = aliasingContext;
    }

    public DynamicContext dynamicContext() {
        return dynamicContext;
    }

    @NotNull
    private TranslationContext contextWithScope(@NotNull JsScope newScope, @NotNull JsBlock block) {
        return contextWithScope(newScope, block, aliasingContext);
    }

    @NotNull
    public TranslationContext contextWithScope(@NotNull JsFunction fun) {
        return contextWithScope(fun, aliasingContext);
    }

    @NotNull
    protected TranslationContext contextWithScope(@NotNull JsScope newScope, @NotNull JsBlock block, @NotNull AliasingContext aliasingContext) {
        return new TranslationContext(staticContext, DynamicContext.newContext(newScope, block), aliasingContext);
    }

    @NotNull
    public TranslationContext contextWithScope(@NotNull JsFunction fun, @NotNull AliasingContext aliasingContext) {
        return contextWithScope(fun.getScope(), fun.getBody(), aliasingContext);
    }

    @NotNull
    public TranslationContext innerBlock(@NotNull JsBlock block) {
        return new TranslationContext(staticContext, dynamicContext.innerBlock(block), aliasingContext);
    }

    @NotNull
    public TranslationContext newDeclaration(@NotNull DeclarationDescriptor descriptor) {
        return contextWithScope(getScopeForDescriptor(descriptor), getBlockForDescriptor(descriptor));
    }

    @NotNull
    public TranslationContext innerContextWithThisAliased(@NotNull DeclarationDescriptor correspondingDescriptor, @NotNull JsName alias) {
        return new TranslationContext(this, aliasingContext.inner(correspondingDescriptor, alias));
    }

    @NotNull
    public TranslationContext innerContextWithAliasesForExpressions(@NotNull Map<JetExpression, JsName> aliases) {
        return new TranslationContext(this, aliasingContext.withAliasesForExpressions(aliases));
    }

    @NotNull
    public TranslationContext innerContextWithDescriptorsAliased(@NotNull Map<DeclarationDescriptor, JsName> aliases) {
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
    public TranslationContext newDeclaration(@NotNull PsiElement element) {
        return newDeclaration(getDescriptorForElement(bindingContext(), element));
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
        JsName alias = aliasingContext.getAliasForDescriptor(descriptor);
        if (alias != null) {
            return alias;
        }
        return staticContext.getNameForDescriptor(descriptor);
    }

    //TODO: util
    @NotNull
    public JsStringLiteral nameToLiteral(@NotNull Named named) {
        return program().getStringLiteral(named.getName().getName());
    }

    @Nullable
    public JsNameRef getQualifierForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.getQualifierForDescriptor(descriptor);
    }

    //TODO: review all invocation with notnull parameters
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

    @NotNull
    public AliasingContext.ThisAliasProvider thisAliasProvider() {
        return aliasingContext().thisAliasProvider;
    }

    @NotNull
    public JsExpression getThisObject(@NotNull DeclarationDescriptor descriptor) {
        JsNameRef ref = thisAliasProvider().get(descriptor);
        return ref == null ? JsLiteral.THIS : ref;
    }
}
