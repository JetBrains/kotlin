/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.context;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.translate.intrinsic.Intrinsics;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver;

import java.util.*;

import static org.jetbrains.kotlin.js.translate.context.UsageTrackerKt.getNameForCapturedDescriptor;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDescriptorForElement;

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
    @Nullable
    private final DeclarationDescriptor declarationDescriptor;
    @Nullable
    private final ClassDescriptor classDescriptor;

    @NotNull
    public static TranslationContext rootContext(@NotNull StaticContext staticContext, JsFunction rootFunction) {
        DynamicContext rootDynamicContext = DynamicContext.rootContext(rootFunction.getScope(), rootFunction.getBody());
        AliasingContext rootAliasingContext = AliasingContext.getCleanContext();
        return new TranslationContext(null, staticContext, rootDynamicContext, rootAliasingContext, null, null, null);
    }

    private final Map<JsExpression, TemporaryConstVariable> expressionToTempConstVariableCache = new HashMap<JsExpression, TemporaryConstVariable>();

    private TranslationContext(
            @Nullable TranslationContext parent,
            @NotNull StaticContext staticContext,
            @NotNull DynamicContext dynamicContext,
            @NotNull AliasingContext aliasingContext,
            @Nullable UsageTracker usageTracker,
            @Nullable DefinitionPlace definitionPlace,
            @Nullable DeclarationDescriptor declarationDescriptor
    ) {
        this.parent = parent;
        this.dynamicContext = dynamicContext;
        this.staticContext = staticContext;
        this.aliasingContext = aliasingContext;
        this.usageTracker = usageTracker;
        this.definitionPlace = definitionPlace;
        this.declarationDescriptor = declarationDescriptor;
        if (declarationDescriptor instanceof ClassDescriptor) {
            this.classDescriptor = (ClassDescriptor) declarationDescriptor;
        }
        else {
            this.classDescriptor = parent != null ? parent.classDescriptor : null;
        }
    }

    @NotNull
    public Map<String, JsName> getImportedModules() {
        return staticContext.getImportedModules();
    }

    @Nullable
    public UsageTracker usageTracker() {
        return usageTracker;
    }

    @NotNull
    public DynamicContext dynamicContext() {
        return dynamicContext;
    }

    @NotNull
    public TranslationContext contextWithScope(@NotNull JsFunction fun) {
        return this.newFunctionBody(fun, aliasingContext, declarationDescriptor);
    }

    @NotNull
    public TranslationContext newFunctionBody(@NotNull JsFunction fun, @Nullable AliasingContext aliasingContext,
            DeclarationDescriptor descriptor) {
        DynamicContext dynamicContext = DynamicContext.newContext(fun.getScope(), fun.getBody());
        if (aliasingContext == null) {
            aliasingContext = this.aliasingContext.inner();
        }

        return new TranslationContext(this, this.staticContext, dynamicContext, aliasingContext, this.usageTracker, null, descriptor);
    }

    @NotNull
    public TranslationContext newFunctionBodyWithUsageTracker(@NotNull JsFunction fun, @NotNull MemberDescriptor descriptor) {
        DynamicContext dynamicContext = DynamicContext.newContext(fun.getScope(), fun.getBody());
        UsageTracker usageTracker = new UsageTracker(this.usageTracker, descriptor, fun.getScope());
        return new TranslationContext(this, this.staticContext, dynamicContext, this.aliasingContext.inner(), usageTracker,
                                      this.definitionPlace, descriptor);
    }

    @NotNull
    public TranslationContext innerWithUsageTracker(@NotNull JsScope scope, @NotNull MemberDescriptor descriptor) {
        UsageTracker usageTracker = new UsageTracker(this.usageTracker, descriptor, scope);
        return new TranslationContext(this, staticContext, dynamicContext, aliasingContext.inner(), usageTracker, definitionPlace,
                                      descriptor);
    }

    @NotNull
    public TranslationContext innerBlock(@NotNull JsBlock block) {
        return new TranslationContext(this, staticContext, dynamicContext.innerBlock(block), aliasingContext, usageTracker, null,
                                      this.declarationDescriptor);
    }

    @NotNull
    public TranslationContext innerBlock() {
        return innerBlock(new JsBlock());
    }

    @NotNull
    public TranslationContext newDeclaration(@NotNull DeclarationDescriptor descriptor, @Nullable DefinitionPlace place) {
        DynamicContext dynamicContext = DynamicContext.newContext(getScopeForDescriptor(descriptor), getBlockForDescriptor(descriptor));
        return new TranslationContext(this, staticContext, dynamicContext, aliasingContext, usageTracker, place, descriptor);
    }

    @NotNull
    private TranslationContext innerWithAliasingContext(AliasingContext aliasingContext) {
        return new TranslationContext(this, staticContext, dynamicContext, aliasingContext, usageTracker, null, declarationDescriptor);
    }

    @NotNull
    public TranslationContext innerContextWithAliased(@NotNull DeclarationDescriptor correspondingDescriptor, @NotNull JsExpression alias) {
        return this.innerWithAliasingContext(aliasingContext.inner(correspondingDescriptor, alias));
    }

    @NotNull
    public TranslationContext innerContextWithAliasesForExpressions(@NotNull Map<KtExpression, JsExpression> aliases) {
        return this.innerWithAliasingContext(aliasingContext.withExpressionsAliased(aliases));
    }

    @NotNull
    public TranslationContext innerContextWithDescriptorsAliased(@NotNull Map<DeclarationDescriptor, JsExpression> aliases) {
        return this.innerWithAliasingContext(aliasingContext.withDescriptorsAliased(aliases));
    }

    @NotNull
    private JsBlock getBlockForDescriptor(@NotNull DeclarationDescriptor descriptor) {
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
    public BindingTrace bindingTrace() {
        return staticContext.getBindingTrace();
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

    @NotNull
    public TemporaryVariable declareTemporary(@Nullable JsExpression initExpression) {
        return dynamicContext.declareTemporary(initExpression);
    }

    @NotNull
    public JsExpression defineTemporary(@NotNull JsExpression initExpression) {
        TemporaryVariable var = dynamicContext.declareTemporary(initExpression);
        addStatementToCurrentBlock(var.assignmentStatement());
        return var.reference();
    }

    @NotNull
    public JsExpression cacheExpressionIfNeeded(@NotNull JsExpression expression) {
        return TranslationUtils.isCacheNeeded(expression) ? defineTemporary(expression) : expression;
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
    public JsProgram program() {
        return staticContext.getProgram();
    }

    @NotNull
    public JsConfig getConfig() {
        return staticContext.getConfig();
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

    public void addStatementsToCurrentBlock(@NotNull Collection<JsStatement> statements) {
        dynamicContext.jsBlock().getStatements().addAll(statements);
    }

    public void addStatementsToCurrentBlockFrom(@NotNull TranslationContext context) {
        addStatementsToCurrentBlockFrom(context.dynamicContext().jsBlock());
    }

    public void addStatementsToCurrentBlockFrom(@NotNull JsBlock block) {
        dynamicContext.jsBlock().getStatements().addAll(block.getStatements());
    }

    public boolean currentBlockIsEmpty() {
        return dynamicContext.jsBlock().isEmpty();
    }

    public void moveVarsFrom(@NotNull TranslationContext context) {
        dynamicContext.moveVarsFrom(context.dynamicContext());
    }

    @NotNull
    public JsBlock getCurrentBlock() {
        return dynamicContext.jsBlock();
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
    public JsExpression getDispatchReceiver(@NotNull ReceiverParameterDescriptor descriptor) {
        JsExpression alias = getAliasForDescriptor(descriptor);
        if (alias != null) {
            return alias;
        }
        if (DescriptorUtils.isObject(descriptor.getContainingDeclaration())) {
            if (isConstructorOrDirectScope(descriptor.getContainingDeclaration())) {
                return JsLiteral.THIS;
            }
            else {
                return getQualifiedReference(descriptor.getContainingDeclaration());
            }
        }

        if (descriptor.getValue() instanceof ExtensionReceiver) return JsLiteral.THIS;

        ClassifierDescriptor classifier = descriptor.getValue().getType().getConstructor().getDeclarationDescriptor();

        // TODO: can't tell why this assertion is valid, revisit this code later
        assert classifier instanceof ClassDescriptor;

        ClassDescriptor cls = (ClassDescriptor) classifier;

        assert classDescriptor != null : "Can't get ReceiverParameterDescriptor in top level";
        JsExpression receiver = getAliasForDescriptor(classDescriptor.getThisAsReceiverParameter());
        if (receiver == null) {
            receiver = JsLiteral.THIS;
        }

        return getDispatchReceiverPath(cls, receiver);
    }

    private boolean isConstructorOrDirectScope(DeclarationDescriptor descriptor) {
        if (declarationDescriptor instanceof ClassDescriptor && !DescriptorUtils.isCompanionObject(declarationDescriptor)) {
            return descriptor == declarationDescriptor;
        }
        else {
            return declarationDescriptor != null && descriptor == DescriptorUtils.getContainingClass(declarationDescriptor);
        }
    }

    @NotNull
    private JsExpression getDispatchReceiverPath(@Nullable ClassDescriptor cls, JsExpression thisExpression) {
        if (cls != null) {
            JsExpression alias = getAliasForDescriptor(cls);
            if (alias != null) {
                return alias;
            }
        }

        if (classDescriptor == cls || parent == null) {
            return thisExpression;
        }

        ClassDescriptor parentDescriptor = parent.classDescriptor;
        if (classDescriptor != parentDescriptor) {
            return new JsNameRef(Namer.OUTER_FIELD_NAME, parent.getDispatchReceiverPath(cls, thisExpression));
        }
        else {
            return parent.getDispatchReceiverPath(cls, thisExpression);
        }
    }

    @NotNull
    public DefinitionPlace getDefinitionPlace() {
        if (definitionPlace != null) return definitionPlace;
        if (parent != null) return parent.getDefinitionPlace();

        throw new AssertionError("Can not find definition place from rootContext(definitionPlace and parent is null)");
    }

    @NotNull
    public JsNameRef define(DeclarationDescriptor descriptor, JsExpression expression) {
        String suggestedName = TranslationUtils.getSuggestedNameForInnerDeclaration(staticContext, descriptor);
        return getDefinitionPlace().define(suggestedName, expression);
    }

    @Nullable
    private JsNameRef captureIfNeedAndGetCapturedName(DeclarationDescriptor descriptor) {
        if (usageTracker != null) {
            usageTracker.used(descriptor);

            JsName name = getNameForCapturedDescriptor(usageTracker, descriptor);
            if (name != null) {
                JsNameRef result = name.makeRef();
                if (shouldCaptureViaThis()) {
                    result.setQualifier(JsLiteral.THIS);
                }
                return result;
            }
        }

        return null;
    }

    private boolean shouldCaptureViaThis() {
        if (declarationDescriptor == null) return false;

        if (DescriptorUtils.isDescriptorWithLocalVisibility(declarationDescriptor)) return false;
        if (declarationDescriptor instanceof ConstructorDescriptor &&
            DescriptorUtils.isDescriptorWithLocalVisibility(declarationDescriptor.getContainingDeclaration())) return false;

        return true;
    }

    @Nullable
    public DeclarationDescriptor getDeclarationDescriptor() {
        return declarationDescriptor;
    }

    public void putClassOrConstructorClosure(@NotNull MemberDescriptor descriptor, @NotNull List<DeclarationDescriptor> closure) {
        staticContext.putClassOrConstructorClosure(descriptor, closure);
    }

    @Nullable
    public List<DeclarationDescriptor> getClassOrConstructorClosure(@NotNull MemberDescriptor classOrConstructor) {
        List<DeclarationDescriptor> result = staticContext.getClassOrConstructorClosure(classOrConstructor);
        if (result == null &&
            classOrConstructor instanceof ConstructorDescriptor &&
            ((ConstructorDescriptor) classOrConstructor).isPrimary()
        ) {
            result = staticContext.getClassOrConstructorClosure((ClassDescriptor) classOrConstructor.getContainingDeclaration());
        }
        return result;
    }

    /**
     * Gets an expression to pass to a constructor of a closure function. I.e. consider the case:
     *
     * ```
     * fun a(x) {
     *     fun b(y) = x + y
     *     return b
     * }
     * ```
     *
     * Here, `x` is a free variable of `b`. Transform `a` into the following form:
     *
     * ```
     * fun a(x) {
     *     fun b0(x0) = { y -> x0 * y }
     *     return b0(x)
     * }
     * ```
     *
     * This function generates arguments passed to newly generated `b0` closure, as well as for the similar case of local class and
     * object expression.
     *
     * @param descriptor represents a free variable or, more generally, free declaration.
     * @return expression to pass to a closure constructor.
     */
    @NotNull
    public JsExpression getArgumentForClosureConstructor(@NotNull DeclarationDescriptor descriptor) {
        JsExpression alias = getAliasForDescriptor(descriptor);
        if (alias != null) return alias;
        if (descriptor instanceof ReceiverParameterDescriptor) {
            return getDispatchReceiver((ReceiverParameterDescriptor) descriptor);
        }
        return getNameForDescriptor(descriptor).makeRef();
    }

    @Nullable
    public JsName getOuterClassReference(ClassDescriptor descriptor) {
        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (!(container instanceof ClassDescriptor) || !descriptor.isInner()) {
            return null;
        }

        return staticContext.getScopeForDescriptor(descriptor).declareName(Namer.OUTER_FIELD_NAME);
    }

    public void startDeclaration() {
        ClassDescriptor classDescriptor = this.classDescriptor;
        if (classDescriptor != null && !(classDescriptor.getContainingDeclaration() instanceof ClassOrPackageFragmentDescriptor)) {
            staticContext.getDeferredCallSites().put(classDescriptor, new ArrayList<DeferredCallSite>());
        }
    }

    @NotNull
    public List<DeferredCallSite> endDeclaration() {
        List<DeferredCallSite> result = null;
        if (classDescriptor != null) {
            result = staticContext.getDeferredCallSites().remove(classDescriptor);
        }
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    public boolean shouldBeDeferred(@NotNull ConstructorDescriptor constructor) {
        ClassDescriptor classDescriptor = constructor.getContainingDeclaration();
        return staticContext.getDeferredCallSites().containsKey(classDescriptor);
    }

    public void deferConstructorCall(@NotNull ConstructorDescriptor constructor, @NotNull List<JsExpression> invocationArgs) {
        ClassDescriptor classDescriptor = constructor.getContainingDeclaration();
        List<DeferredCallSite> callSites = staticContext.getDeferredCallSites().get(classDescriptor);
        if (callSites == null) throw new IllegalStateException("This method should be call only when `shouldBeDeferred` method " +
                                                               "reports true for given constructor: " + constructor);
        callSites.add(new DeferredCallSite(constructor, invocationArgs, this));
    }

    @Nullable
    public JsExpression getModuleExpressionFor(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.getModuleExpressionFor(descriptor);
    }
}
