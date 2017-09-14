/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.naming.SuggestedName;
import org.jetbrains.kotlin.js.translate.declaration.ClassModelGenerator;
import org.jetbrains.kotlin.js.translate.intrinsic.Intrinsics;
import org.jetbrains.kotlin.js.translate.reference.CallExpressionTranslator;
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator;
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.js.translate.utils.UtilsKt;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.kotlin.serialization.js.ModuleKind;

import java.util.*;

import static org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsKt.isCoroutineLambda;
import static org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsKt.shouldBeExported;
import static org.jetbrains.kotlin.js.translate.context.UsageTrackerKt.getNameForCapturedDescriptor;
import static org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isNativeObject;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDescriptorForElement;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn;

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
    private final DeclarationDescriptor declarationDescriptor;
    @Nullable
    private final ClassDescriptor classDescriptor;
    @Nullable
    private final VariableDescriptor continuationParameterDescriptor;

    @Nullable
    private InlineFunctionContext inlineFunctionContext;

    @NotNull
    public static TranslationContext rootContext(@NotNull StaticContext staticContext) {
        DynamicContext rootDynamicContext = DynamicContext.rootContext(
                staticContext.getFragment().getScope(), staticContext.getFragment().getInitializerBlock());
        AliasingContext rootAliasingContext = AliasingContext.getCleanContext();
        return new TranslationContext(null, staticContext, rootDynamicContext, rootAliasingContext, null, null);
    }

    private final Map<JsExpression, TemporaryConstVariable> expressionToTempConstVariableCache = new HashMap<>();

    private TranslationContext(
            @Nullable TranslationContext parent,
            @NotNull StaticContext staticContext,
            @NotNull DynamicContext dynamicContext,
            @NotNull AliasingContext aliasingContext,
            @Nullable UsageTracker usageTracker,
            @Nullable DeclarationDescriptor declarationDescriptor
    ) {
        this.parent = parent;
        this.dynamicContext = dynamicContext;
        this.staticContext = staticContext;
        this.aliasingContext = aliasingContext;
        this.usageTracker = usageTracker;
        this.declarationDescriptor = declarationDescriptor;
        if (declarationDescriptor instanceof ClassDescriptor) {
            this.classDescriptor = (ClassDescriptor) declarationDescriptor;
        }
        else {
            this.classDescriptor = parent != null ? parent.classDescriptor : null;
        }

        continuationParameterDescriptor = calculateContinuationParameter();
        inlineFunctionContext = parent != null ? parent.inlineFunctionContext : null;

        DeclarationDescriptor parentDescriptor = parent != null ? parent.declarationDescriptor : null;
        if (parentDescriptor != declarationDescriptor &&
            declarationDescriptor instanceof CallableDescriptor &&
            InlineUtil.isInline(declarationDescriptor)) {
            inlineFunctionContext = new InlineFunctionContext((CallableDescriptor) declarationDescriptor);
        }
    }

    private VariableDescriptor calculateContinuationParameter() {
        if (parent != null && parent.declarationDescriptor == declarationDescriptor) {
            return parent.continuationParameterDescriptor;
        }
        if (declarationDescriptor instanceof FunctionDescriptor) {
            FunctionDescriptor function = (FunctionDescriptor) declarationDescriptor;
            if (function.isSuspend()) {
                ClassDescriptor continuationDescriptor =
                        DescriptorUtilKt.findContinuationClassDescriptor(getCurrentModule(), NoLookupLocation.FROM_BACKEND);

                return new LocalVariableDescriptor(
                        declarationDescriptor,
                        Annotations.Companion.getEMPTY(),
                        Name.identifier("continuation"),
                        continuationDescriptor.getDefaultType(),
                        SourceElement.NO_SOURCE);
            }
        }
        return null;
    }

    @Nullable
    public UsageTracker usageTracker() {
        return usageTracker;
    }

    @NotNull
    public DynamicContext dynamicContext() {
        return dynamicContext;
    }

    @Nullable
    public InlineFunctionContext getInlineFunctionContext() {
        return inlineFunctionContext;
    }

    @NotNull
    public TranslationContext contextWithScope(@NotNull JsFunction fun) {
        return this.newFunctionBody(fun, aliasingContext, declarationDescriptor);
    }

    @NotNull
    private TranslationContext newFunctionBody(
            @NotNull JsFunction fun, @Nullable AliasingContext aliasingContext,
            DeclarationDescriptor descriptor
    ) {
        DynamicContext dynamicContext = DynamicContext.newContext(fun.getScope(), fun.getBody());
        if (aliasingContext == null) {
            aliasingContext = this.aliasingContext.inner();
        }

        return new TranslationContext(this, this.staticContext, dynamicContext, aliasingContext, this.usageTracker, descriptor);
    }

    @NotNull
    public TranslationContext newFunctionBodyWithUsageTracker(@NotNull JsFunction fun, @NotNull MemberDescriptor descriptor) {
        DynamicContext dynamicContext = DynamicContext.newContext(fun.getScope(), fun.getBody());
        UsageTracker usageTracker = new UsageTracker(this.usageTracker, descriptor);
        return new TranslationContext(this, this.staticContext, dynamicContext, this.aliasingContext.inner(), usageTracker, descriptor);
    }

    @NotNull
    public TranslationContext innerWithUsageTracker(@NotNull MemberDescriptor descriptor) {
        UsageTracker usageTracker = new UsageTracker(this.usageTracker, descriptor);
        return new TranslationContext(this, staticContext, dynamicContext, aliasingContext.inner(), usageTracker, descriptor);
    }

    @NotNull
    public TranslationContext inner(@NotNull MemberDescriptor descriptor) {
        return new TranslationContext(this, staticContext, dynamicContext, aliasingContext.inner(), usageTracker, descriptor);
    }

    @NotNull
    public TranslationContext innerBlock(@NotNull JsBlock block) {
        return new TranslationContext(this, staticContext, dynamicContext.innerBlock(block), aliasingContext, usageTracker,
                                      this.declarationDescriptor);
    }

    @NotNull
    public TranslationContext innerBlock() {
        return innerBlock(new JsBlock());
    }

    @NotNull
    public TranslationContext newDeclaration(@NotNull DeclarationDescriptor descriptor) {
        JsBlock innerBlock = getBlockForDescriptor(descriptor);
        if (innerBlock == null) {
            innerBlock = dynamicContext.jsBlock();
        }
        DynamicContext dynamicContext = DynamicContext.newContext(getScopeForDescriptor(descriptor), innerBlock);
        return new TranslationContext(this, staticContext, dynamicContext, aliasingContext, usageTracker, descriptor);
    }

    @NotNull
    private TranslationContext innerWithAliasingContext(AliasingContext aliasingContext) {
        return new TranslationContext(this, staticContext, dynamicContext, aliasingContext, usageTracker, declarationDescriptor);
    }

    @NotNull
    public TranslationContext innerContextWithAliased(@NotNull DeclarationDescriptor correspondingDescriptor, @NotNull JsExpression alias) {
        return this.innerWithAliasingContext(aliasingContext.inner(correspondingDescriptor, alias));
    }

    @NotNull
    public TranslationContext innerContextWithAliasesForExpressions(@NotNull Map<KtExpression, JsExpression> aliases) {
        if (aliases.isEmpty()) return this;
        return this.innerWithAliasingContext(aliasingContext.withExpressionsAliased(aliases));
    }

    @NotNull
    public TranslationContext innerContextWithDescriptorsAliased(@NotNull Map<DeclarationDescriptor, JsExpression> aliases) {
        if (aliases.isEmpty()) return this;
        return this.innerWithAliasingContext(aliasingContext.withDescriptorsAliased(aliases));
    }

    @Nullable
    private JsBlock getBlockForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableDescriptor) {
            return getFunctionObject((CallableDescriptor) descriptor).getBody();
        }
        else {
            return null;
        }
    }

    @Nullable
    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
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
    public JsName getInnerNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.getInnerNameForDescriptor(descriptor);
    }

    @NotNull
    public JsName getInlineableInnerNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        JsName name;
        if (inlineFunctionContext == null || !isPublicInlineFunction() || !shouldBeExported(descriptor, getConfig()) ||
            DescriptorUtils.isAncestor(inlineFunctionContext.getDescriptor(), descriptor, false)) {
            name = getInnerNameForDescriptor(descriptor);
        }
        else {
            String tag = staticContext.getTag(descriptor);
            name = inlineFunctionContext.getImports().computeIfAbsent(tag, t -> {
                JsExpression imported = createInlineLocalImportExpression(descriptor);
                if (imported instanceof JsNameRef) {
                    JsNameRef importedNameRef = (JsNameRef) imported;
                    if (importedNameRef.getQualifier() == null && importedNameRef.getIdent().equals(Namer.getRootPackageName()) &&
                        (descriptor instanceof PackageFragmentDescriptor || descriptor instanceof ModuleDescriptor)) {
                        return importedNameRef.getName();
                    }
                }

                JsName result = JsScope.declareTemporaryName(StaticContext.getSuggestedName(descriptor));
                if (isFromCurrentModule(descriptor) && !AnnotationsUtils.isNativeObject(descriptor)) {
                    MetadataProperties.setLocalAlias(result, getInnerNameForDescriptor(descriptor));
                }
                MetadataProperties.setDescriptor(result, descriptor);
                MetadataProperties.setStaticRef(result, imported);
                MetadataProperties.setImported(result, true);
                inlineFunctionContext.getImportBlock().getStatements().add(JsAstUtils.newVar(result, imported));
                return result;
            });
        }

        return name;
    }

    @NotNull
    public JsName getNameForObjectInstance(@NotNull ClassDescriptor descriptor) {
        return staticContext.getNameForObjectInstance(descriptor);
    }

    @NotNull
    public JsExpression getQualifiedReference(@NotNull DeclarationDescriptor descriptor) {
        JsExpression result = staticContext.getQualifiedReference(descriptor);
        if (isPublicInlineFunction()) {
            if (isFromCurrentModule(descriptor)) {
                if (descriptor instanceof MemberDescriptor) {
                    staticContext.export((MemberDescriptor) descriptor, true);
                }
            }
            else {
                ModuleDescriptor module = DescriptorUtils.getContainingModule(descriptor);
                if (module != staticContext.getCurrentModule() && !isInlineFunction(descriptor)) {
                    JsExpression replacement = staticContext.exportModuleForInline(module);
                    if (replacement != null) {
                        result = replaceModuleReference(result, getInnerNameForDescriptor(module), replacement);
                    }
                }
            }
        }
        return result;
    }

    private boolean isInlineFunction(@NotNull DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof CallableDescriptor)) return false;
        return CallExpressionTranslator.shouldBeInlined((CallableDescriptor) descriptor, this);
    }

    private static JsExpression replaceModuleReference(
            @NotNull JsExpression expression,
            @NotNull JsName expectedModuleName,
            @NotNull JsExpression reexportExpr
    ) {
        if (expression instanceof JsNameRef) {
            JsNameRef nameRef = (JsNameRef) expression;
            if (nameRef.getQualifier() == null) {
                return expectedModuleName == nameRef.getName() ? reexportExpr : expression;
            }
            else {
                JsExpression newQualifier = replaceModuleReference(nameRef.getQualifier(), expectedModuleName, reexportExpr);
                if (newQualifier == nameRef.getQualifier()) {
                    return expression;
                }
                JsExpression result = nameRef.getName() != null ?
                                      new JsNameRef(nameRef.getName(), newQualifier) :
                                      new JsNameRef(nameRef.getIdent(), newQualifier);
                result.copyMetadataFrom(nameRef);
                return result;
            }
        }
        else {
            return expression;
        }
    }

    @NotNull
    public JsExpression getInnerReference(@NotNull DeclarationDescriptor descriptor) {
        return pureFqn(getInlineableInnerNameForDescriptor(descriptor), null);
    }

    @NotNull
    private JsExpression createInlineLocalImportExpression(@NotNull DeclarationDescriptor descriptor) {
        JsExpression result = getQualifiedReference(descriptor);
        JsName name = getInnerNameForDescriptor(descriptor);

        SuggestedName suggested = staticContext.suggestName(descriptor);
        if (suggested != null && getConfig().getModuleKind() != ModuleKind.PLAIN && isPublicInlineFunction()) {
            String moduleId = AnnotationsUtils.getModuleName(suggested.getDescriptor());
            if (moduleId != null) {
                JsExpression replacement = staticContext.exportModuleForInline(moduleId, name);
                result = replaceModuleReference(result, name, replacement);
            }
            else if (isNativeObject(suggested.getDescriptor()) && DescriptorUtils.isTopLevelDeclaration(suggested.getDescriptor())) {
                String fileModuleId = AnnotationsUtils.getFileModuleName(bindingContext(), suggested.getDescriptor());
                if (fileModuleId != null) {
                    JsName fileModuleName = staticContext.getImportedModule(fileModuleId, null).getInternalName();
                    JsExpression replacement = staticContext.exportModuleForInline(fileModuleId, fileModuleName);
                    result = replaceModuleReference(staticContext.getQualifiedReference(descriptor), fileModuleName, replacement);
                }
            }
        }

        return result;
    }

    @NotNull
    public JsName getNameForBackingField(@NotNull VariableDescriptorWithAccessors property) {
        return staticContext.getNameForBackingField(property);
    }

    @NotNull
    public TemporaryVariable declareTemporary(@Nullable JsExpression initExpression, @Nullable Object source) {
        return dynamicContext.declareTemporary(initExpression, source);
    }

    @NotNull
    public JsExpression defineTemporary(@NotNull JsExpression initExpression) {
        TemporaryVariable var = dynamicContext.declareTemporary(initExpression, initExpression.getSource());
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
            TemporaryVariable tmpVar = declareTemporary(expression, expression.getSource());

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
        JsExpression nameRef = captureIfNeedAndGetCapturedName(descriptor);
        if (nameRef != null) {
            return nameRef;
        }

        JsExpression alias = aliasingContext.getAliasForDescriptor(descriptor);
        return alias != null ? alias.deepCopy() : null;
    }

    @NotNull
    public JsExpression getDispatchReceiver(@NotNull ReceiverParameterDescriptor descriptor) {
        JsExpression alias = getAliasForDescriptor(descriptor);
        if (alias != null) {
            return alias;
        }
        if (isCoroutineLambda(descriptor.getContainingDeclaration())) {
            JsNameRef result = new JsNameRef("$$controller$$", JsAstUtils.stateMachineReceiver());
            MetadataProperties.setCoroutineController(result, true);
            return result;
        }

        if (DescriptorUtils.isObject(descriptor.getContainingDeclaration())) {
            if (isConstructorOrDirectScope(descriptor.getContainingDeclaration())) {
                return new JsThisRef();
            }
            else {
                ClassDescriptor objectDescriptor = (ClassDescriptor) descriptor.getContainingDeclaration();
                return ReferenceTranslator.translateAsValueReference(objectDescriptor, this);
            }
        }

        if (descriptor.getValue() instanceof ExtensionReceiver) return new JsThisRef();

        ClassifierDescriptor classifier = descriptor.getValue().getType().getConstructor().getDeclarationDescriptor();

        // TODO: can't tell why this assertion is valid, revisit this code later
        assert classifier instanceof ClassDescriptor;

        ClassDescriptor cls = (ClassDescriptor) classifier;

        assert classDescriptor != null : "Can't get ReceiverParameterDescriptor in top level";
        JsExpression receiver = getAliasForDescriptor(classDescriptor.getThisAsReceiverParameter());
        if (receiver == null) {
            receiver = new JsThisRef();
        }

        return getDispatchReceiverPath(cls, receiver);
    }

    private boolean isConstructorOrDirectScope(DeclarationDescriptor descriptor) {
        return descriptor == DescriptorUtils.getParentOfType(declarationDescriptor, ClassDescriptor.class, false);
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

        if (classDescriptor != parent.classDescriptor) {
            return new JsNameRef(Namer.OUTER_FIELD_NAME, parent.getDispatchReceiverPath(cls, thisExpression));
        }
        else {
            return parent.getDispatchReceiverPath(cls, thisExpression);
        }
    }

    @Nullable
    private JsExpression captureIfNeedAndGetCapturedName(@NotNull DeclarationDescriptor descriptor) {
        if (usageTracker != null) {
            usageTracker.used(descriptor);

            JsName name = getNameForCapturedDescriptor(usageTracker, descriptor);
            if (name != null) return getCapturedReference(name);
        }

        return null;
    }

    @Nullable
    public JsExpression captureTypeIfNeedAndGetCapturedName(@NotNull TypeParameterDescriptor descriptor) {
        if (usageTracker == null) return null;

        usageTracker.used(descriptor);

        JsName name = usageTracker.getCapturedTypes().get(descriptor);
        return name != null ? getCapturedReference(name) : null;
    }

    @NotNull
    public JsName getCapturedTypeName(@NotNull TypeParameterDescriptor descriptor) {
        JsName result = usageTracker != null ? usageTracker.getCapturedTypes().get(descriptor) : null;
        if (result == null) {
            result = getNameForDescriptor(descriptor);
        }

        return result;
    }

    @NotNull
    private JsExpression getCapturedReference(@NotNull JsName name) {
        JsExpression result;
        if (shouldCaptureViaThis()) {
            result = new JsThisRef();
            int depth = getOuterLocalClassDepth();
            for (int i = 0; i < depth; ++i) {
                result = new JsNameRef(Namer.OUTER_FIELD_NAME, result);
            }
            result = new JsNameRef(name, result);
        }
        else {
            result = name.makeRef();
        }
        return result;
    }

    private int getOuterLocalClassDepth() {
        if (usageTracker == null) return 0;
        MemberDescriptor capturingDescriptor = usageTracker.getContainingDescriptor();
        if (!(capturingDescriptor instanceof ClassDescriptor)) return 0;

        ClassDescriptor capturingClassDescriptor = (ClassDescriptor) capturingDescriptor;
        ClassDescriptor currentDescriptor = classDescriptor;
        if (currentDescriptor == null) return 0;

        int depth = 0;
        while (currentDescriptor != capturingClassDescriptor) {
            DeclarationDescriptor container = currentDescriptor.getContainingDeclaration();
            if (!(container instanceof ClassDescriptor)) return 0;
            currentDescriptor = (ClassDescriptor) container;
            depth++;
        }
        return depth;
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
        if (classOrConstructor instanceof TypeAliasConstructorDescriptor) {
            ClassConstructorDescriptor constructorDescriptor = ((TypeAliasConstructorDescriptor) classOrConstructor).getUnderlyingConstructorDescriptor();
            return getClassOrConstructorClosure(constructorDescriptor);
        }

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
        if (isCoroutineLambda(descriptor)) {
            return new JsThisRef();
        }
        return getNameForDescriptor(descriptor).makeRef();
    }

    @NotNull
    public JsExpression getTypeArgumentForClosureConstructor(@NotNull TypeParameterDescriptor descriptor) {
        JsExpression captured = null;
        if (usageTracker != null) {
            JsName name = usageTracker.getCapturedTypes().get(descriptor);
            if (name != null) {
                captured = name.makeRef();
            }
        }

        return captured != null ? captured : getNameForDescriptor(descriptor).makeRef();
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
            staticContext.getDeferredCallSites().put(classDescriptor, new ArrayList<>());
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

    public boolean shouldBeDeferred(@NotNull ClassConstructorDescriptor constructor) {
        ClassDescriptor classDescriptor = constructor.getContainingDeclaration();
        return staticContext.getDeferredCallSites().containsKey(classDescriptor);
    }

    public void deferConstructorCall(@NotNull ClassConstructorDescriptor constructor, @NotNull List<JsExpression> invocationArgs) {
        ClassDescriptor classDescriptor = constructor.getContainingDeclaration();
        List<DeferredCallSite> callSites = staticContext.getDeferredCallSites().get(classDescriptor);
        if (callSites == null) throw new IllegalStateException("This method should be call only when `shouldBeDeferred` method " +
                                                               "reports true for given constructor: " + constructor);
        callSites.add(new DeferredCallSite(constructor, invocationArgs, this));
    }

    public void addInlineCall(@NotNull CallableDescriptor descriptor) {
        staticContext.addInlineCall(descriptor);
    }

    public void addDeclarationStatement(@NotNull JsStatement statement) {
        if (inlineFunctionContext != null) {
            inlineFunctionContext.getDeclarationsBlock().getStatements().add(statement);
        }
        else {
            staticContext.getDeclarationStatements().add(statement);
        }
    }

    public void addTopLevelStatement(@NotNull JsStatement statement) {
        staticContext.getTopLevelStatements().add(statement);
    }

    @NotNull
    public JsFunction createRootScopedFunction(@NotNull DeclarationDescriptor descriptor) {
        return createRootScopedFunction(descriptor.toString());
    }

    @NotNull
    public JsFunction createRootScopedFunction(@NotNull String description) {
        return new JsFunction(staticContext.getFragment().getScope(), new JsBlock(), description);
    }

    public void addClass(@NotNull ClassDescriptor classDescriptor) {
        if (inlineFunctionContext != null) {
            JsClassModel classModel = new ClassModelGenerator(this).generateClassModel(classDescriptor);
            List<JsStatement> targetStatements = inlineFunctionContext.getPrototypeBlock().getStatements();
            JsName superName = classModel.getSuperName();
            if (superName != null) {
                targetStatements.addAll(UtilsKt.createPrototypeStatements(superName, classModel.getName()));
            }
            targetStatements.addAll(classModel.getPostDeclarationBlock().getStatements());
        }
        else {
            staticContext.addClass(classDescriptor);
        }
    }

    public void export(@NotNull MemberDescriptor descriptor) {
        staticContext.export(descriptor, false);
    }

    public boolean isFromCurrentModule(@NotNull DeclarationDescriptor descriptor) {
        return staticContext.getCurrentModule() == DescriptorUtilsKt.getModule(descriptor);
    }

    public boolean isPublicInlineFunction() {
        if (inlineFunctionContext == null) return false;

        return shouldBeExported(inlineFunctionContext.getDescriptor(), getConfig());
    }

    @Nullable
    public VariableDescriptor getContinuationParameterDescriptor() {
        return continuationParameterDescriptor;
    }

    @NotNull
    public ModuleDescriptor getCurrentModule() {
        return staticContext.getCurrentModule();
    }

    @Nullable
    public TranslationContext getParent() {
        return parent;
    }

    @NotNull
    public JsName getNameForSpecialFunction(@NotNull SpecialFunction function) {
        if (inlineFunctionContext == null || !isPublicInlineFunction()) {
            return staticContext.getNameForSpecialFunction(function);
        }
        else {
            String tag = TranslationUtils.getTagForSpecialFunction(function);
            return inlineFunctionContext.getImports().computeIfAbsent(tag, t -> {
                JsExpression imported = Namer.createSpecialFunction(function);
                JsName result = JsScope.declareTemporaryName(function.getSuggestedName());
                MetadataProperties.setImported(result, true);
                MetadataProperties.setSpecialFunction(result, function);
                inlineFunctionContext.getImportBlock().getStatements().add(JsAstUtils.newVar(result, imported));
                return result;
            });
        }
    }
}
