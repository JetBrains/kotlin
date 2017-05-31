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
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.hash.LinkedHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.FunctionTypesKt;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.naming.NameSuggestion;
import org.jetbrains.kotlin.js.naming.SuggestedName;
import org.jetbrains.kotlin.js.translate.context.generator.Generator;
import org.jetbrains.kotlin.js.translate.context.generator.Rule;
import org.jetbrains.kotlin.js.translate.declaration.ClassModelGenerator;
import org.jetbrains.kotlin.js.translate.intrinsic.Intrinsics;
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.SignatureUtilsKt;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallsKt;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.serialization.deserialization.FindClassInModuleKt;
import org.jetbrains.kotlin.serialization.js.ModuleKind;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.js.config.JsConfig.UNKNOWN_EXTERNAL_MODULE_NAME;
import static org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isLibraryObject;
import static org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isNativeObject;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn;
import static org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getSuperclass;

/**
 * Aggregates all the static parts of the context.
 */
public final class StaticContext {
    @NotNull
    private final JsProgram program;

    @NotNull
    private final JsProgramFragment fragment;

    @NotNull
    private final BindingTrace bindingTrace;
    @NotNull
    private final Namer namer;

    @NotNull
    private final Intrinsics intrinsics;

    @NotNull
    private final JsScope rootScope;

    @NotNull
    private final Generator<JsName> innerNames = new InnerNameGenerator();
    @NotNull
    private final Map<FqName, JsName> packageNames = Maps.newHashMap();
    @NotNull
    private final Generator<JsScope> scopes = new ScopeGenerator();
    @NotNull
    private final Generator<JsName> objectInstanceNames = new ObjectInstanceNameGenerator();

    @NotNull
    private final Map<JsScope, JsFunction> scopeToFunction = Maps.newHashMap();

    @NotNull
    private final Map<MemberDescriptor, List<DeclarationDescriptor>> classOrConstructorClosure = Maps.newHashMap();

    @NotNull
    private final Map<ClassDescriptor, List<DeferredCallSite>> deferredCallSites = new HashMap<>();

    @NotNull
    private final JsConfig config;

    @NotNull
    private final ModuleDescriptor currentModule;

    @NotNull
    private final NameSuggestion nameSuggestion = new NameSuggestion();

    @NotNull
    private final Map<DeclarationDescriptor, JsName> nameCache = new HashMap<>();

    @NotNull
    private final Map<PropertyDescriptor, JsName> backingFieldNameCache = new HashMap<>();

    @NotNull
    private final Map<DeclarationDescriptor, JsExpression> fqnCache = new HashMap<>();

    private final Map<DeclarationDescriptor, String> tagCache = new HashMap<>();

    @NotNull
    private final Map<JsImportedModuleKey, JsImportedModule> importedModules = new LinkedHashMap<>();

    @NotNull
    private final JsScope rootPackageScope;

    @NotNull
    private final DeclarationExporter exporter = new DeclarationExporter(this);

    @NotNull
    private final Map<FqName, JsScope> packageScopes = new HashMap<>();

    @NotNull
    private final ClassModelGenerator classModelGenerator;

    @Nullable
    private JsName nameForImportsForInline;

    public StaticContext(
            @NotNull BindingTrace bindingTrace,
            @NotNull JsConfig config,
            @NotNull ModuleDescriptor moduleDescriptor
    ) {
        program = new JsProgram();
        JsFunction rootFunction = JsAstUtils.createFunctionWithEmptyBody(program.getScope());
        fragment = new JsProgramFragment(rootFunction.getScope());

        this.bindingTrace = bindingTrace;
        this.namer = Namer.newInstance(program.getRootScope());
        this.intrinsics = new Intrinsics(this);
        this.rootScope = fragment.getScope();
        this.config = config;
        this.currentModule = moduleDescriptor;
        rootPackageScope = new JsObjectScope(rootScope, "<root package>");

        JsName kotlinName = rootScope.declareName(Namer.KOTLIN_NAME);
        createImportedModule(new JsImportedModuleKey(Namer.KOTLIN_LOWER_NAME, null), Namer.KOTLIN_LOWER_NAME, kotlinName, null);

        classModelGenerator = new ClassModelGenerator(this);
    }

    @NotNull
    public JsProgram getProgram() {
        return program;
    }

    @NotNull
    public JsProgramFragment getFragment() {
        return fragment;
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

    @Nullable
    public String getTag(@NotNull DeclarationDescriptor descriptor) {
        String tag;
        if (!tagCache.containsKey(descriptor)) {
            tag = SignatureUtilsKt.generateSignature(descriptor);
            tagCache.put(descriptor, tag);
        }
        else {
            tag = tagCache.get(descriptor);
        }
        return tag;
    }

    @NotNull
    private JsExpression getQualifiedExpression(@NotNull DeclarationDescriptor descriptor) {
        JsExpression fqn = fqnCache.computeIfAbsent(descriptor, this::buildQualifiedExpression);
        return fqn.deepCopy();
    }

    @Nullable
    public SuggestedName suggestName(@NotNull DeclarationDescriptor descriptor) {
        return nameSuggestion.suggest(descriptor);
    }

    @NotNull
    private JsExpression buildQualifiedExpression(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            KotlinType type = classDescriptor.getDefaultType();
            if (KotlinBuiltIns.isAny(classDescriptor)) {
                return pureFqn("Object", null);
            }
            else if (KotlinBuiltIns.isInt(type) || KotlinBuiltIns.isShort(type) || KotlinBuiltIns.isByte(type) ||
                     KotlinBuiltIns.isFloat(type) || KotlinBuiltIns.isDouble(type)) {
                return pureFqn("Number", null);
            }
            else if (KotlinBuiltIns.isLong(type)) {
                return pureFqn("Long", Namer.kotlinObject());
            }
            else if (KotlinBuiltIns.isChar(type)) {
                return pureFqn("BoxedChar", Namer.kotlinObject());
            }
            else if (KotlinBuiltIns.isString(type)) {
                return pureFqn("String", null);
            }
            else if (KotlinBuiltIns.isBoolean(type)) {
                return pureFqn("Boolean", null);
            }
            else if (KotlinBuiltIns.isArrayOrPrimitiveArray(classDescriptor)) {
                return pureFqn("Array", null);
            }
            else if (FunctionTypesKt.isBuiltinFunctionalType(type)) {
                return pureFqn("Function", null);
            }
            else if (TypeUtilsKt.isThrowable(classDescriptor.getDefaultType())) {
                return pureFqn("Error", null);
            }
        }

        SuggestedName suggested = suggestName(descriptor);
        if (suggested == null) {
            ModuleDescriptor module = DescriptorUtils.getContainingModule(descriptor);
            JsExpression result = getModuleExpressionFor(module);
            return result != null ? result : pureFqn(Namer.getRootPackageName(), null);
        }

        if (config.getModuleKind() != ModuleKind.PLAIN) {
            String moduleName = AnnotationsUtils.getModuleName(suggested.getDescriptor());
            if (moduleName != null) {
                return JsAstUtils.pureFqn(getImportedModule(moduleName, suggested.getDescriptor()).getInternalName(), null);
            }
        }

        JsExpression expression;
        List<JsName> partNames = getActualNameFromSuggested(suggested);
        if (isLibraryObject(suggested.getDescriptor())) {
            expression = Namer.kotlinObject();
        }
        // Don't generate qualifier for top-level native declarations
        // Don't generate qualifier for local declarations
        else if (isNativeObject(suggested.getDescriptor()) && !isNativeObject(suggested.getScope()) ||
                 suggested.getDescriptor() instanceof CallableDescriptor && suggested.getScope() instanceof FunctionDescriptor) {
            expression = null;
        }
        else {
            expression = getQualifiedExpression(suggested.getScope());
        }

        if (isNativeObject(suggested.getDescriptor()) && DescriptorUtils.isTopLevelDeclaration(suggested.getDescriptor())) {
            String fileModuleName = AnnotationsUtils.getFileModuleName(getBindingContext(), suggested.getDescriptor());
            if (fileModuleName != null) {
                JsName moduleJsName = getImportedModule(fileModuleName, null).getInternalName();
                expression = pureFqn(moduleJsName, expression);
            }

            String qualifier = AnnotationsUtils.getFileQualifier(getBindingContext(), suggested.getDescriptor());
            if (qualifier != null) {
                for (String qualifierPart : StringUtil.split(qualifier, ".")) {
                    expression = pureFqn(qualifierPart, expression);
                }
            }
        }

        for (JsName partName : partNames) {
            expression = new JsNameRef(partName, expression);
            applySideEffects(expression, suggested.getDescriptor());
        }
        assert expression != null : "Since partNames is not empty, expression must be non-null";
        return expression;
    }

    @NotNull
    public JsNameRef getQualifiedReference(@NotNull FqName packageFqName) {
        JsName packageName = getNameForPackage(packageFqName);
        return pureFqn(packageName, packageFqName.isRoot() ? null : getQualifierForParentPackage(packageFqName.parent()));
    }

    @NotNull
    public JsName getNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor && KotlinBuiltIns.isAny((ClassDescriptor) descriptor)) {
            JsName result = rootScope.declareName("Object");
            MetadataProperties.setDescriptor(result, descriptor);
            return result;
        }
        SuggestedName suggested = nameSuggestion.suggest(descriptor);
        if (suggested == null) {
            throw new IllegalArgumentException("Can't generate name for root declarations: " + descriptor);
        }
        return getActualNameFromSuggested(suggested).get(0);
    }

    @NotNull
    public JsName getNameForBackingField(@NotNull PropertyDescriptor property) {
        JsName name = backingFieldNameCache.get(property);

        if (name == null) {
            SuggestedName fqn = nameSuggestion.suggest(property);
            assert fqn != null : "Properties are non-root declarations: " + property;
            assert fqn.getNames().size() == 1 : "Private names must always consist of exactly one name";

            JsScope scope = getScopeForDescriptor(fqn.getScope());
            String baseName = NameSuggestion.getPrivateMangledName(fqn.getNames().get(0), property) + "_0";
            name = scope.declareFreshName(baseName);
            backingFieldNameCache.put(property, name);
        }

        return name;
    }

    @NotNull
    public JsName getInnerNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        JsName name = innerNames.get(descriptor.getOriginal());
        assert name != null : "Must have inner name for descriptor";
        return name;
    }

    @NotNull
    public JsName getNameForObjectInstance(@NotNull ClassDescriptor descriptor) {
        JsName name = objectInstanceNames.get(descriptor.getOriginal());
        assert name != null : "Must have inner name for object instance";
        return name;
    }

    @NotNull
    private List<JsName> getActualNameFromSuggested(@NotNull SuggestedName suggested) {
        JsScope scope = getScopeForDescriptor(suggested.getScope());

        if (DynamicCallsKt.isDynamic(suggested.getDescriptor())) {
            scope = JsDynamicScope.INSTANCE;
        }
        else if (AnnotationsUtils.isPredefinedObject(suggested.getDescriptor()) &&
                 DescriptorUtils.isTopLevelDeclaration(suggested.getDescriptor())) {
            scope = rootScope;
        }

        List<JsName> names = new ArrayList<>();
        if (suggested.getStable()) {
            String tag = getTag(suggested.getDescriptor());
            int index = 0;
            for (String namePart : suggested.getNames()) {
                JsName name = scope.declareName(namePart);
                MetadataProperties.setDescriptor(name, suggested.getDescriptor());
                if (tag != null && !AnnotationsUtils.isNativeObject(suggested.getDescriptor()) &&
                    !AnnotationsUtils.isLibraryObject(suggested.getDescriptor())
                ) {
                    fragment.getNameBindings().add(new JsNameBinding(index++ + ":" + tag, name));
                }
                names.add(name);
            }
        }
        else {
            // TODO: consider using sealed class to represent FQNs
            assert suggested.getNames().size() == 1 : "Private names must always consist of exactly one name";
            JsName name = nameCache.get(suggested.getDescriptor());
            if (name == null) {
                String baseName = NameSuggestion.sanitizeName(suggested.getNames().get(0));
                if (suggested.getDescriptor() instanceof LocalVariableDescriptor ||
                    suggested.getDescriptor() instanceof ValueParameterDescriptor
                ) {
                    name = JsScope.declareTemporaryName(baseName);
                }
                else {
                    if (!DescriptorUtils.isDescriptorWithLocalVisibility(suggested.getDescriptor())) {
                        baseName += "_0";
                    }
                    name = scope.declareFreshName(baseName);
                }
            }
            nameCache.put(suggested.getDescriptor(), name);
            MetadataProperties.setDescriptor(name, suggested.getDescriptor());
            String tag = getTag(suggested.getDescriptor());
            if (tag != null) {
                fragment.getNameBindings().add(new JsNameBinding(tag, name));
            }
            names.add(name);
        }

        return names;
    }

    @NotNull
    private JsName getNameForPackage(@NotNull FqName packageFqName) {
        return ContainerUtil.getOrCreate(packageNames, packageFqName, (Factory<JsName>) () -> {
            String name = Namer.generatePackageName(packageFqName);
            return rootPackageScope.declareName(name);
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
    public JsName importDeclaration(@NotNull String suggestedName, @NotNull String tag, @NotNull JsExpression declaration) {
        JsName result = importDeclarationImpl(suggestedName, tag, declaration);
        fragment.getNameBindings().add(new JsNameBinding(tag, result));
        return result;
    }

    @NotNull
    private JsName importDeclarationImpl(@NotNull String suggestedName, @NotNull String tag, @NotNull JsExpression declaration) {
        JsName result = JsScope.declareTemporaryName(suggestedName);
        MetadataProperties.setImported(result, true);
        fragment.getImports().put(tag, declaration);
        return result;
    }

    @NotNull
    private JsName localOrImportedName(@NotNull DeclarationDescriptor descriptor, @NotNull String suggestedName) {
        ModuleDescriptor module = DescriptorUtilsKt.getModule(descriptor);
        JsName name;
        String tag = getTag(descriptor);
        boolean isNative =  AnnotationsUtils.isNativeObject(descriptor) || AnnotationsUtils.isLibraryObject(descriptor);
        if (module != currentModule && !isLocallyRedeclaredBuiltin(descriptor) || isNative) {
            assert tag != null : "Can't import declaration without tag: " + descriptor;
            JsNameRef result = getQualifiedReference(descriptor);
            if (isNative && result.getQualifier() == null && result.getName() != null) {
                name = result.getName();
                tag = null;
            }
            else {
                name = importDeclarationImpl(suggestedName, tag, result);
            }
        }
        else {
            name = JsScope.declareTemporaryName(suggestedName);
        }
        if (tag != null) {
            fragment.getNameBindings().add(new JsNameBinding(tag, name));
        }
        MetadataProperties.setDescriptor(name, descriptor);
        return name;
    }

    // When compiling stdlib, we may have sources for built-in declaration. In this case we have two distinct descriptors
    // for declaration with one signature. One descriptor is from current module, another descriptor is from built-in module.
    // Different declarations refer different descriptors. This may cause single name to be both imported and declared locally,
    // which in turn causes runtime error. We avoid this by detecting this case and turning off import.
    private boolean isLocallyRedeclaredBuiltin(@NotNull DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof ClassDescriptor)) return false;
        FqName fqName = DescriptorUtils.getFqNameSafe(descriptor);
        ClassId classId = ClassId.topLevel(fqName);
        ClassDescriptor localDescriptor = FindClassInModuleKt.findClassAcrossModuleDependencies(currentModule, classId);
        return localDescriptor != null && DescriptorUtils.getContainingModule(localDescriptor) == currentModule;
    }

    private final class InnerNameGenerator extends Generator<JsName> {
        public InnerNameGenerator() {
            addRule(descriptor -> {
                if (descriptor instanceof FunctionDescriptor) {
                    FunctionDescriptor initialDescriptor = ((FunctionDescriptor) descriptor).getInitialSignatureDescriptor();
                    if (initialDescriptor != null) {
                        return getInnerNameForDescriptor(initialDescriptor);
                    }
                }
                if (descriptor instanceof ModuleDescriptor) {
                    return getModuleInnerName(descriptor);
                }
                if (descriptor instanceof LocalVariableDescriptor || descriptor instanceof ParameterDescriptor) {
                    return getNameForDescriptor(descriptor);
                }
                if (descriptor instanceof ConstructorDescriptor) {
                    if (((ConstructorDescriptor) descriptor).isPrimary()) {
                        return getInnerNameForDescriptor(((ConstructorDescriptor) descriptor).getConstructedClass());
                    }
                }
                return localOrImportedName(descriptor, getSuggestedName(descriptor));
            });
        }
    }

    private final class ObjectInstanceNameGenerator extends Generator<JsName> {
        public ObjectInstanceNameGenerator() {
            addRule(descriptor -> {
                String suggested = getSuggestedName(descriptor) + Namer.OBJECT_INSTANCE_FUNCTION_SUFFIX;
                JsName result = JsScope.declareTemporaryName(suggested);
                String tag = SignatureUtilsKt.generateSignature(descriptor);
                if (tag != null) {
                    fragment.getNameBindings().add(new JsNameBinding("object:" + tag, result));
                }
                return result;
            });
        }
    }

    @NotNull
    public static String getSuggestedName(@NotNull DeclarationDescriptor descriptor) {
        String suggestedName;
        if (descriptor instanceof PropertyGetterDescriptor) {
            PropertyGetterDescriptor getter = (PropertyGetterDescriptor) descriptor;
            suggestedName = "get_" + getSuggestedName(getter.getCorrespondingProperty());
        }
        else if (descriptor instanceof PropertySetterDescriptor) {
            PropertySetterDescriptor setter = (PropertySetterDescriptor) descriptor;
            suggestedName = "set_" + getSuggestedName(setter.getCorrespondingProperty());
        }
        else if (descriptor instanceof ConstructorDescriptor) {
            ConstructorDescriptor constructor = (ConstructorDescriptor) descriptor;
            suggestedName = getSuggestedName(constructor.getContainingDeclaration()) + "_init";
            descriptor = descriptor.getContainingDeclaration();
            assert descriptor != null : "ConstructorDescriptor should have containing declaration: " + constructor;
        }
        else {
            if (descriptor.getName().isSpecial()) {
                if (descriptor instanceof ClassDescriptor) {
                    if (DescriptorUtils.isAnonymousObject(descriptor)) {
                        suggestedName = "ObjectLiteral";
                    }
                    else {
                        suggestedName = "Anonymous";
                    }
                }
                else if (descriptor instanceof FunctionDescriptor) {
                    suggestedName = "lambda";
                }
                else {
                    suggestedName = "anonymous";
                }
            }
            else {
                suggestedName = NameSuggestion.sanitizeName(descriptor.getName().asString());
            }
        }

        if (!(descriptor instanceof PackageFragmentDescriptor) && !DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            DeclarationDescriptor container = descriptor.getContainingDeclaration();
            assert container != null : "We just figured out that descriptor is not for a top-level declaration: " + descriptor;
            suggestedName = getSuggestedName(container) + "$" + NameSuggestion.sanitizeName(suggestedName);
        }

        return suggestedName;
    }

    private JsScope getScopeForPackage(FqName fqName) {
        JsScope scope = packageScopes.get(fqName);
        if (scope == null) {
            if (fqName.isRoot()) {
                scope = new JsRootScope(program);
            }
            else {
                JsScope parentScope = getScopeForPackage(fqName.parent());
                scope = parentScope.innerObjectScope(fqName.shortName().asString());
            }
            packageScopes.put(fqName, scope);
        }
        return scope;
    }

    private final class ScopeGenerator extends Generator<JsScope> {

        public ScopeGenerator() {
            Rule<JsScope> generateNewScopesForClassesWithNoAncestors = descriptor -> {
                if (!(descriptor instanceof ClassDescriptor)) {
                    return null;
                }
                if (getSuperclass((ClassDescriptor) descriptor) == null) {
                    JsFunction function = new JsFunction(new JsRootScope(program), new JsBlock(), descriptor.toString());
                    scopeToFunction.put(function.getScope(), function);
                    return function.getScope();
                }
                return null;
            };
            Rule<JsScope> generateInnerScopesForDerivedClasses = descriptor -> {
                if (!(descriptor instanceof ClassDescriptor)) {
                    return null;
                }
                ClassDescriptor superclass = getSuperclass((ClassDescriptor) descriptor);
                if (superclass == null) {
                    return null;
                }
                return getScopeForDescriptor(superclass).innerObjectScope("Scope for class " + descriptor.getName());
            };
            Rule<JsScope> generateNewScopesForPackageDescriptors = descriptor -> fragment.getScope();
            //TODO: never get there
            Rule<JsScope> generateInnerScopesForMembers =
                    descriptor -> fragment.getScope().innerObjectScope("Scope for member " + descriptor.getName());
            Rule<JsScope> createFunctionObjectsForCallableDescriptors = descriptor -> {
                if (!(descriptor instanceof CallableDescriptor)) {
                    return null;
                }

                JsFunction correspondingFunction = JsAstUtils.createFunctionWithEmptyBody(fragment.getScope());
                assert (!scopeToFunction.containsKey(correspondingFunction.getScope())) : "Scope to function value overridden for " + descriptor;
                scopeToFunction.put(correspondingFunction.getScope(), correspondingFunction);
                return correspondingFunction.getScope();
            };
            Rule<JsScope> scopeForPackage = descriptor -> {
                if (!(descriptor instanceof PackageFragmentDescriptor)) return null;

                PackageFragmentDescriptor packageDescriptor = (PackageFragmentDescriptor) descriptor;
                return getScopeForPackage(packageDescriptor.getFqName());
            };
            addRule(scopeForPackage);
            addRule(createFunctionObjectsForCallableDescriptors);
            addRule(generateNewScopesForClassesWithNoAncestors);
            addRule(generateInnerScopesForDerivedClasses);
            addRule(generateNewScopesForPackageDescriptors);
            addRule(generateInnerScopesForMembers);
        }
    }

    @Nullable
    private JsExpression getModuleExpressionFor(@NotNull DeclarationDescriptor descriptor) {
        JsName name = getModuleInnerName(descriptor);
        return name != null ? JsAstUtils.pureFqn(name, null) : null;
    }

    @Nullable
    private JsName getModuleInnerName(@NotNull DeclarationDescriptor descriptor) {
        ModuleDescriptor module = DescriptorUtils.getContainingModule(descriptor);
        if (currentModule == module) {
            return rootScope.declareName(Namer.getRootPackageName());
        }
        String moduleName = suggestModuleName(module);

        if (UNKNOWN_EXTERNAL_MODULE_NAME.equals(moduleName)) return null;

        return getImportedModule(moduleName, null).getInternalName();
    }

    @NotNull
    public static String suggestModuleName(@NotNull ModuleDescriptor module) {
        if (module == module.getBuiltIns().getBuiltInsModule()) {
            return Namer.KOTLIN_LOWER_NAME;
        }
        else {
            String moduleName = module.getName().asString();
            return moduleName.substring(1, moduleName.length() - 1);
        }
    }

    @NotNull
    public JsImportedModule getImportedModule(@NotNull String baseName, @Nullable DeclarationDescriptor descriptor) {
        String plainName = descriptor != null && config.getModuleKind() == ModuleKind.UMD ? getPlainId(descriptor) : null;
        JsImportedModuleKey key = new JsImportedModuleKey(baseName, plainName);

        JsImportedModule module = importedModules.get(key);
        if (module == null) {
            JsName internalName = JsScope.declareTemporaryName(Namer.LOCAL_MODULE_PREFIX + Namer.suggestedModuleName(baseName));
            module = createImportedModule(key, baseName, internalName, plainName != null ? pureFqn(plainName, null) : null);
        }
        return module;
    }

    private JsImportedModule createImportedModule(JsImportedModuleKey key, String baseName, JsName internalName, JsExpression plainName) {
        JsImportedModule module = new JsImportedModule(baseName, internalName, plainName);
        importedModules.put(key, module);
        fragment.getImportedModules().add(module);
        return module;
    }

    @NotNull
    private String getPlainId(@NotNull DeclarationDescriptor declaration) {
        SuggestedName suggestedName = nameSuggestion.suggest(declaration);
        assert suggestedName != null : "Declaration should not be ModuleDescriptor, therefore suggestedName should be non-null";
        return suggestedName.getNames().get(0);
    }

    private static void applySideEffects(JsExpression expression, DeclarationDescriptor descriptor) {
        if (descriptor instanceof FunctionDescriptor ||
            descriptor instanceof PackageFragmentDescriptor ||
            descriptor instanceof ClassDescriptor
        ) {
            MetadataProperties.setSideEffects(expression, SideEffectKind.PURE);
        }
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

    @NotNull
    public List<JsStatement> getTopLevelStatements() {
        return fragment.getInitializerBlock().getStatements();
    }

    @NotNull
    public List<JsStatement> getDeclarationStatements() {
        return fragment.getDeclarationBlock().getStatements();
    }

    public void addClass(@NotNull ClassDescriptor classDescriptor) {
        if (!AnnotationsUtils.isNativeObject(classDescriptor) && !AnnotationsUtils.isLibraryObject(classDescriptor)) {
            fragment.getClasses().put(getInnerNameForDescriptor(classDescriptor), classModelGenerator.generateClassModel(classDescriptor));
        }
    }

    public void export(@NotNull MemberDescriptor descriptor, boolean force) {
        exporter.export(descriptor, force);
    }

    @NotNull
    public NameSuggestion getNameSuggestion() {
        return nameSuggestion;
    }

    @NotNull
    public ModuleDescriptor getCurrentModule() {
        return currentModule;
    }

    public void addInlineCall(@NotNull CallableDescriptor descriptor) {
        String tag = Namer.getFunctionTag(descriptor, config);
        fragment.getInlineModuleMap().put(tag, getModuleExpressionFor(descriptor));
    }

    @NotNull
    public JsName getNameForImportsForInline() {
        if (nameForImportsForInline == null) {
            JsName name = JsScope.declareTemporaryName(Namer.IMPORTS_FOR_INLINE_PROPERTY);
            fragment.getNameBindings().add(new JsNameBinding(Namer.IMPORTS_FOR_INLINE_PROPERTY, name));
            nameForImportsForInline = name;
            return name;
        }
        else {
            return nameForImportsForInline;
        }
    }
}
