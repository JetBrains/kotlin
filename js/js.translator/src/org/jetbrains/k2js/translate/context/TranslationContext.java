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
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForElement;

/**
 * @author Pavel Talanov
 *         <p/>
 *         All the info about the state of the translation process.
 */
public final class TranslationContext {

    @NotNull
    private final DynamicContext dynamicContext;
    @NotNull
    private final StaticContext staticContext;

    @NotNull
    public static TranslationContext rootContext(@NotNull StaticContext staticContext) {
        JsProgram program = staticContext.getProgram();
        JsBlock globalBlock = program.getGlobalBlock();
        return new TranslationContext(staticContext,
                                      DynamicContext.rootContext(staticContext.getRootScope(), globalBlock));
    }

    private TranslationContext(@NotNull StaticContext staticContext, @NotNull DynamicContext dynamicContext) {
        this.dynamicContext = dynamicContext;
        this.staticContext = staticContext;
    }

    @NotNull
    public TranslationContext contextWithScope(@NotNull NamingScope newScope, @NotNull JsBlock block) {
        return new TranslationContext(staticContext, DynamicContext.newContext(newScope, block));
    }

    @NotNull
    public TranslationContext innerBlock(@NotNull JsBlock block) {
        return new TranslationContext(staticContext, dynamicContext.innerBlock(block));
    }

    @NotNull
    public TranslationContext newDeclaration(@NotNull DeclarationDescriptor descriptor) {
        return contextWithScope(getScopeForDescriptor(descriptor), getBlockForDescriptor(descriptor));
    }

    //TODO: consider passing a function here
    @NotNull
    public TranslationContext innerContextWithGivenScopeAndBlock(@NotNull JsScope scope, @NotNull JsBlock block) {
        return contextWithScope(dynamicContext.getScope().innerScope(scope), block);
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
    public TranslationContext newDeclaration(@NotNull PsiElement element) {
        return newDeclaration(getDescriptorForElement(bindingContext(), element));
    }

    @NotNull
    public BindingContext bindingContext() {
        return staticContext.getBindingContext();
    }

    @NotNull
    public NamingScope getScopeForDescriptor(@NotNull DeclarationDescriptor descriptor) {
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

    @Nullable
    public JsNameRef getQualifierForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.getQualifierForDescriptor(descriptor.getOriginal());
    }

    @NotNull
    public TemporaryVariable declareTemporary(@NotNull JsExpression initExpression) {
        return dynamicContext.declareTemporary(initExpression);
    }

    @NotNull
    public Aliaser aliaser() {
        return staticContext.getAliaser();
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
    public JsScope jsScope() {
        return dynamicContext.jsScope();
    }

    @NotNull
    public JsFunction getFunctionObject(@NotNull CallableDescriptor descriptor) {
        return staticContext.getFunctionWithScope(descriptor);
    }

    public void addStatementToCurrentBlock(@NotNull JsStatement statement) {
        dynamicContext.jsBlock().getStatements().add(statement);
    }
}
