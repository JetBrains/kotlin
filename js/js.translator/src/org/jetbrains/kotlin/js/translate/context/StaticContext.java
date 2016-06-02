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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.metadata.HasMetadata;
import com.google.dart.compiler.backend.js.ast.metadata.MetadataProperties;
import com.google.dart.compiler.backend.js.ast.metadata.SideEffectKind;
import com.intellij.openapi.util.Factory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.hash.LinkedHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.naming.FQNGenerator;
import org.jetbrains.kotlin.js.naming.FQNPart;
import org.jetbrains.kotlin.js.translate.context.generator.Generator;
import org.jetbrains.kotlin.js.translate.context.generator.Rule;
import org.jetbrains.kotlin.js.translate.intrinsic.Intrinsics;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallsKt;

import java.util.*;

import static org.jetbrains.kotlin.js.config.LibrarySourcesConfig.UNKNOWN_EXTERNAL_MODULE_NAME;
import static org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isLibraryObject;
import static org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isNativeObject;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn;
import static org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getContainingDeclaration;
import static org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getSuperclass;

/**
 * Aggregates all the static parts of the context.
 */
public final class StaticContext {

    public static StaticContext generateStaticContext(
            @NotNull BindingTrace bindingTrace,
            @NotNull JsConfig config,
            @NotNull ModuleDescriptor moduleDescriptor) {
        JsProgram program = new JsProgram("main");
        Namer namer = Namer.newInstance(program.getRootScope());
        Intrinsics intrinsics = new Intrinsics();
        StandardClasses standardClasses = StandardClasses.bindImplementations(namer.getKotlinScope());
        return new StaticContext(program, bindingTrace, namer, intrinsics, standardClasses, program.getRootScope(), config,
                                 moduleDescriptor);
    }

    @NotNull
    private final JsProgram program;

    @NotNull
    private final BindingTrace bindingTrace;
    @NotNull
    private final Namer namer;

    @NotNull
    private final Intrinsics intrinsics;

    @NotNull
    private final StandardClasses standardClasses;

    @NotNull
    private final JsScope rootScope;

    @NotNull
    private final Map<FqName, JsName> packageNames = Maps.newHashMap();
    @NotNull
    private final Generator<JsScope> scopes = new ScopeGenerator();

    @NotNull
    private final Map<JsScope, JsFunction> scopeToFunction = Maps.newHashMap();

    @NotNull
    private final Map<MemberDescriptor, List<DeclarationDescriptor>> classOrConstructorClosure = Maps.newHashMap();

    @NotNull
    private final Map<ClassDescriptor, List<DeferredCallSite>> deferredCallSites = new HashMap<ClassDescriptor, List<DeferredCallSite>>();

    @NotNull
    private final JsConfig config;

    @NotNull
    private final ModuleDescriptor currentModule;

    @NotNull
    private final FQNGenerator fqnGenerator = new FQNGenerator();

    @NotNull
    private final Map<DeclarationDescriptor, JsName> nameCache = new HashMap<DeclarationDescriptor, JsName>();

    private final Map<JsScope, Map<String, JsName>> persistentNames = new HashMap<JsScope, Map<String, JsName>>();

    @NotNull
    private final Map<DeclarationDescriptor, JsExpression> fqnCache = new HashMap<DeclarationDescriptor, JsExpression>();

    @NotNull
    private final Map<String, JsName> importedModules = new LinkedHashMap<String, JsName>();

    private Map<String, JsName> readOnlyImportedModules;

    //TODO: too many parameters in constructor
    private StaticContext(
            @NotNull JsProgram program,
            @NotNull BindingTrace bindingTrace,
            @NotNull Namer namer,
            @NotNull Intrinsics intrinsics,
            @NotNull StandardClasses standardClasses,
            @NotNull JsScope rootScope,
            @NotNull JsConfig config,
            @NotNull ModuleDescriptor moduleDescriptor
    ) {
        this.program = program;
        this.bindingTrace = bindingTrace;
        this.namer = namer;
        this.intrinsics = intrinsics;
        this.rootScope = rootScope;
        this.standardClasses = standardClasses;
        this.config = config;
        currentModule = moduleDescriptor;
    }

    @NotNull
    public JsProgram getProgram() {
        return program;
    }

    @NotNull
    public BindingTrace getBindingTrace() {
        return bindingTrace;
    }

    @NotNull
    public BindingContext getBindingContext() {
        return bindingTrace.getBindingContext();
    }

    @NotNull
    public Intrinsics getIntrinsics() {
        return intrinsics;
    }

    @NotNull
    public Namer getNamer() {
        return namer;
    }

    @NotNull
    private JsScope getRootScope() {
        return rootScope;
    }

    @NotNull
    public Map<String, JsName> getImportedModules() {
        if (readOnlyImportedModules == null) {
            readOnlyImportedModules = Collections.unmodifiableMap(importedModules);
        }
        return readOnlyImportedModules;
    }

    @NotNull
    public JsScope getScopeForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof ModuleDescriptor) {
            return rootScope;
        }
        JsScope scope = scopes.get(descriptor.getOriginal());
        assert scope != null : "Must have a scope for descriptor";
        return scope;
    }

    @NotNull
    public JsFunction getFunctionWithScope(@NotNull CallableDescriptor descriptor) {
        JsScope scope = getScopeForDescriptor(descriptor);
        JsFunction function = scopeToFunction.get(scope);
        assert scope.equals(function.getScope()) : "Inconsistency.";
        return function;
    }

    @NotNull
    public JsNameRef getQualifiedReference(@NotNull DeclarationDescriptor descriptor) {
        return (JsNameRef) getQualifiedExpression(descriptor);
    }

    @NotNull
    private JsExpression getQualifiedExpression(@NotNull DeclarationDescriptor descriptor) {
        JsExpression fqn = fqnCache.get(descriptor);
        if (fqn == null) {
            fqn = buildQualifiedExpression(descriptor);
            fqnCache.put(descriptor, fqn);
        }
        return fqn.deepCopy();
    }

    @NotNull
    private JsExpression buildQualifiedExpression(@NotNull DeclarationDescriptor descriptor) {
        FQNPart part = fqnGenerator.generate(descriptor);
        if (part.getDescriptor() instanceof ModuleDescriptor) {
            ModuleDescriptor module = DescriptorUtils.getContainingModule(descriptor);
            if (currentModule == module) {
                return pureFqn(Namer.getRootPackageName(), null);
            }
            else {
                JsExpression result = getModuleExpressionFor(module);
                return result != null ? result : JsAstUtils.pureFqn(rootScope.declareName(Namer.getRootPackageName()), null);
            }
        }
        else {
            JsExpression expression;
            List<JsName> partNames;
            if (standardClasses.isStandardObject(part.getDescriptor())) {
                expression = Namer.kotlinObject();
                partNames = Collections.singletonList(standardClasses.getStandardObjectName(part.getDescriptor()));
            }
            else if (isLibraryObject(part.getDescriptor())) {
                expression = Namer.kotlinObject();
                partNames = getNameForFQNPart(part);
            }
            else if (isNative(part.getDescriptor()) && !isNativeObject(part.getScope())) {
                expression = null;
                partNames = getNameForFQNPart(part);
            }
            else {
                if (part.getDescriptor() instanceof CallableDescriptor && part.getScope() instanceof FunctionDescriptor) {
                    expression = null;
                }
                else {
                    expression = getQualifiedExpression(part.getScope());
                }
                partNames = getNameForFQNPart(part);
            }
            for (JsName partName : partNames) {
                expression = new JsNameRef(partName, expression);
                applySideEffects(expression, part.getDescriptor());
            }
            assert expression != null : "Since partNames is not empty, expression must be non-null";
            return expression;
        }
    }

    private static boolean isNative(DeclarationDescriptor descriptor) {
        if (isNativeObject(descriptor)) return true;

        if (descriptor instanceof PropertyAccessorDescriptor) {
            PropertyAccessorDescriptor accessor = (PropertyAccessorDescriptor) descriptor;
            return isNative(accessor.getCorrespondingProperty());
        }

        return false;
    }

    @NotNull
    public JsNameRef getQualifiedReference(@NotNull FqName packageFqName) {
        JsName packageName = getNameForPackage(packageFqName);
        return pureFqn(packageName, packageFqName.isRoot() ? null : getQualifierForParentPackage(packageFqName.parent()));
    }

    @NotNull
    public JsName getNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return getNameForFQNPart(fqnGenerator.generate(descriptor)).get(0);
    }

    @NotNull
    private List<JsName> getNameForFQNPart(@NotNull FQNPart part) {
        JsScope scope = getScopeForDescriptor(part.getScope());

        if (DynamicCallsKt.isDynamic(part.getDescriptor())) {
            scope = JsDynamicScope.INSTANCE;
        }

        List<JsName> names = new ArrayList<JsName>();
        if (part.getShared()) {
            Map<String, JsName> scopeNames = persistentNames.get(scope);
            if (scopeNames == null) {
                scopeNames = new HashMap<String, JsName>();
                persistentNames.put(scope, scopeNames);
            }
            for (String namePart : part.getNames()) {
                JsName name = scopeNames.get(namePart);
                if (name == null) {
                    name = scope.declareName(namePart);
                    scopeNames.put(namePart, name);
                }
                names.add(name);
            }
        }
        else {
            // TODO: consider using sealed class to represent FQNs
            assert part.getNames().size() == 1 : "Private names must always consist of exactly one name";
            JsName name = nameCache.get(part.getDescriptor());
            if (name == null) {
                String baseName = part.getNames().get(0);
                if (!DescriptorUtils.isDescriptorWithLocalVisibility(part.getDescriptor())) {
                    baseName += "_0";
                }
                name = scope.declareFreshName(baseName);
            }
            nameCache.put(part.getDescriptor(), name);
            names.add(name);
        }

        return names;
    }

    @NotNull
    public JsName getNameForPackage(@NotNull final FqName packageFqName) {
        return ContainerUtil.getOrCreate(packageNames, packageFqName, new Factory<JsName>() {
            @Override
            public JsName create() {
                String name = Namer.generatePackageName(packageFqName);
                return getRootScope().declareName(name);
            }
        });
    }

    @NotNull
    private JsNameRef getQualifierForParentPackage(@NotNull FqName packageFqName) {
        JsNameRef result = null;
        JsNameRef qualifier = null;

        FqName fqName = packageFqName;

        while (true) {
            JsNameRef ref = pureFqn(getNameForPackage(fqName), null);

            if (qualifier == null) {
                result = ref;
            }
            else {
                qualifier.setQualifier(ref);
            }

            qualifier = ref;

            if (fqName.isRoot()) break;
            fqName = fqName.parent();
        }

        return result;
    }

    @NotNull
    public JsConfig getConfig() {
        return config;
    }

    @NotNull
    public JsName declarePropertyOrPropertyAccessorName(@NotNull DeclarationDescriptor descriptor, @NotNull String name, boolean fresh) {
        JsScope scope = getEnclosingScope(descriptor);
        return fresh ? scope.declareFreshName(name) : scope.declareName(name);
    }

    @NotNull
    private JsScope getEnclosingScope(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = getContainingDeclaration(descriptor);
        return getScopeForDescriptor(containingDeclaration.getOriginal());
    }

    private final class ScopeGenerator extends Generator<JsScope> {

        public ScopeGenerator() {
            Rule<JsScope> generateNewScopesForClassesWithNoAncestors = new Rule<JsScope>() {
                @Override
                public JsScope apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof ClassDescriptor)) {
                        return null;
                    }
                    if (getSuperclass((ClassDescriptor) descriptor) == null) {
                        return getRootScope().innerObjectScope("Scope for class " + descriptor.getName());
                    }
                    return null;
                }
            };
            Rule<JsScope> generateInnerScopesForDerivedClasses = new Rule<JsScope>() {
                @Override
                public JsScope apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof ClassDescriptor)) {
                        return null;
                    }
                    ClassDescriptor superclass = getSuperclass((ClassDescriptor) descriptor);
                    if (superclass == null) {
                        return null;
                    }
                    return getScopeForDescriptor(superclass).innerObjectScope("Scope for class " + descriptor.getName());
                }
            };
            Rule<JsScope> generateNewScopesForPackageDescriptors = new Rule<JsScope>() {
                @Override
                public JsScope apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof PackageFragmentDescriptor)) {
                        return null;
                    }
                    return getRootScope().innerObjectScope("Package " + descriptor.getName());
                }
            };
            //TODO: never get there
            Rule<JsScope> generateInnerScopesForMembers = new Rule<JsScope>() {
                @Override
                public JsScope apply(@NotNull DeclarationDescriptor descriptor) {
                    JsScope enclosingScope = getEnclosingScope(descriptor);
                    return enclosingScope.innerObjectScope("Scope for member " + descriptor.getName());
                }
            };
            Rule<JsScope> createFunctionObjectsForCallableDescriptors = new Rule<JsScope>() {
                @Override
                public JsScope apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof CallableDescriptor)) {
                        return null;
                    }
                    JsScope enclosingScope = getEnclosingScope(descriptor);

                    JsFunction correspondingFunction = JsAstUtils.createFunctionWithEmptyBody(enclosingScope);
                    assert (!scopeToFunction.containsKey(correspondingFunction.getScope())) : "Scope to function value overridden for " + descriptor;
                    scopeToFunction.put(correspondingFunction.getScope(), correspondingFunction);
                    return correspondingFunction.getScope();
                }
            };
            addRule(createFunctionObjectsForCallableDescriptors);
            addRule(generateNewScopesForClassesWithNoAncestors);
            addRule(generateInnerScopesForDerivedClasses);
            addRule(generateNewScopesForPackageDescriptors);
            addRule(generateInnerScopesForMembers);
        }
    }

    @Nullable
    public JsExpression getModuleExpressionFor(@NotNull DeclarationDescriptor descriptor) {
        ModuleDescriptor module = DescriptorUtils.getContainingModule(descriptor);
        if (currentModule == module) {
            return pureFqn(Namer.getRootPackageName(), null);
        }
        String moduleName;
        if (module == module.getBuiltIns().getBuiltInsModule()) {
            moduleName = Namer.KOTLIN_LOWER_NAME;
        }
        else {
            moduleName = module.getName().asString();
            moduleName = moduleName.substring(1, moduleName.length() - 1);
        }

        if (UNKNOWN_EXTERNAL_MODULE_NAME.equals(moduleName)) return null;

        JsName moduleId = moduleName.equals(Namer.KOTLIN_LOWER_NAME) ? rootScope.declareName(Namer.KOTLIN_NAME) :
                          importedModules.get(moduleName);
        if (moduleId == null) {
            moduleId = rootScope.declareFreshName(Namer.LOCAL_MODULE_PREFIX + Namer.suggestedModuleName(moduleName));
            importedModules.put(moduleName, moduleId);
        }

        return JsAstUtils.pureFqn(moduleId, null);
    }


    private static JsExpression applySideEffects(JsExpression expression, DeclarationDescriptor descriptor) {
        if (expression instanceof HasMetadata) {
            if (descriptor instanceof FunctionDescriptor ||
                descriptor instanceof PackageFragmentDescriptor ||
                descriptor instanceof ClassDescriptor
            ) {
                MetadataProperties.setSideEffects((HasMetadata) expression, SideEffectKind.PURE);
            }
        }
        return expression;
    }

    public void putClassOrConstructorClosure(@NotNull MemberDescriptor localClass, @NotNull List<DeclarationDescriptor> closure) {
        classOrConstructorClosure.put(localClass, Lists.newArrayList(closure));
    }

    @Nullable
    public List<DeclarationDescriptor> getClassOrConstructorClosure(@NotNull MemberDescriptor descriptor) {
        List<DeclarationDescriptor> result = classOrConstructorClosure.get(descriptor);
        return result != null ? Lists.newArrayList(result) : null;
    }

    @NotNull
    public Map<ClassDescriptor, List<DeferredCallSite>> getDeferredCallSites() {
        return deferredCallSites;
    }
}
