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

import com.google.common.collect.Maps;
import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.k2js.translate.context.generator.Generator;
import org.jetbrains.k2js.translate.context.generator.Rule;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;
import org.jetbrains.k2js.translate.utils.DescriptorUtils;
import org.jetbrains.k2js.translate.utils.JsAstUtils;
import org.jetbrains.k2js.translate.utils.PredefinedAnnotation;

import java.util.Map;

import static org.jetbrains.k2js.translate.utils.AnnotationsUtils.*;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.*;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Aggregates all the static parts of the context.
 */
public final class StaticContext {

    public static StaticContext generateStaticContext(@NotNull JetStandardLibrary library,
                                                      @NotNull BindingContext bindingContext) {
        JsProgram program = new JsProgram("main");
        JsRootScope jsRootScope = program.getRootScope();
        Namer namer = Namer.newInstance(jsRootScope);
        NamingScope scope = NamingScope.rootScope(jsRootScope);
        Intrinsics intrinsics = Intrinsics.standardLibraryIntrinsics(library);
        StandardClasses standardClasses =
            StandardClasses.bindImplementations(namer.getKotlinScope());
        return new StaticContext(program, bindingContext, namer, intrinsics, standardClasses, scope);
    }

    @NotNull
    private final JsProgram program;

    @NotNull
    private final BindingContext bindingContext;
    @NotNull
    private final Namer namer;

    @NotNull
    private final Intrinsics intrinsics;

    @NotNull
    private final StandardClasses standardClasses;

    @NotNull
    private final NamingScope rootScope;

    @NotNull
    private final Generator<JsName> names = new NameGenerator();
    @NotNull
    private final Generator<NamingScope> scopes = new ScopeGenerator();
    @NotNull
    private final Generator<JsNameRef> qualifiers = new QualifierGenerator();
    @NotNull
    private final Generator<Boolean> qualifierIsNull = new QualifierIsNullGenerator();
    @NotNull
    private final Map<NamingScope, JsFunction> scopeToFunction = Maps.newHashMap();


    //TODO: too many parameters in constructor
    private StaticContext(@NotNull JsProgram program, @NotNull BindingContext bindingContext,
                          @NotNull Namer namer, @NotNull Intrinsics intrinsics,
                          @NotNull StandardClasses standardClasses, @NotNull NamingScope rootScope) {
        this.program = program;
        this.bindingContext = bindingContext;
        this.namer = namer;
        this.intrinsics = intrinsics;
        this.rootScope = rootScope;
        this.standardClasses = standardClasses;
    }

    @NotNull
    public JsProgram getProgram() {
        return program;
    }

    @NotNull
    public BindingContext getBindingContext() {
        return bindingContext;
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
    public NamingScope getRootScope() {
        return rootScope;
    }

    @NotNull
    public NamingScope getScopeForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        NamingScope namingScope = scopes.get(descriptor.getOriginal());
        assert namingScope != null : "Must have a scope for descriptor";
        return namingScope;
    }

    @NotNull
    public JsFunction getFunctionWithScope(@NotNull CallableDescriptor descriptor) {
        NamingScope scope = getScopeForDescriptor(descriptor);
        JsFunction function = scopeToFunction.get(scope);
        assert scope.jsScope().equals(function.getScope()) : "Inconsistency.";
        return function;
    }

    @NotNull
    public JsName getNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        JsName name = names.get(descriptor.getOriginal());
        assert name != null : "Must have name for descriptor";
        return name;
    }

    private final class NameGenerator extends Generator<JsName> {
        public NameGenerator() {
            Rule<JsName> namesForStandardClasses = new Rule<JsName>() {
                @Override
                @Nullable
                public JsName apply(@NotNull DeclarationDescriptor data) {
                    if (!standardClasses.isStandardObject(data)) {
                        return null;
                    }
                    return standardClasses.getStandardObjectName(data);
                }
            };
            Rule<JsName> namespacesShouldBeDefinedInRootScope = new Rule<JsName>() {
                @Override
                @Nullable
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof NamespaceDescriptor)) {
                        return null;
                    }
                    String nameForNamespace = getNameForNamespace((NamespaceDescriptor)descriptor);
                    return getRootScope().declareUnobfuscatableName(nameForNamespace);
                }
            };
            Rule<JsName> memberDeclarationsInsideParentsScope = new Rule<JsName>() {
                @Override
                @Nullable
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    NamingScope namingScope = getEnclosingScope(descriptor);
                    return namingScope.declareObfuscatableName(descriptor.getName());
                }
            };
            Rule<JsName> constructorHasTheSameNameAsTheClass = new Rule<JsName>() {
                @Override
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof ConstructorDescriptor)) {
                        return null;
                    }
                    ClassDescriptor containingClass = getContainingClass(descriptor);
                    assert containingClass != null : "Can't have constructor without a class";
                    return getNameForDescriptor(containingClass);
                }
            };
            Rule<JsName> accessorsHasNamesWithSpecialPrefixes = new Rule<JsName>() {
                @Override
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof PropertyAccessorDescriptor)) {
                        return null;
                    }
                    boolean isGetter = descriptor instanceof PropertyGetterDescriptor;
                    String propertyName = ((PropertyAccessorDescriptor)descriptor).getCorrespondingProperty().getName();
                    String accessorName = Namer.getNameForAccessor(propertyName, isGetter);
                    NamingScope enclosingScope = getEnclosingScope(descriptor);
                    return enclosingScope.declareObfuscatableName(accessorName);
                }
            };

            Rule<JsName> predefinedObjectsHasUnobfuscatableNames = new Rule<JsName>() {
                @Override
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    for (PredefinedAnnotation annotation : PredefinedAnnotation.values()) {
                        if (!hasAnnotationOrInsideAnnotatedClass(descriptor, annotation)) {
                            continue;
                        }
                        String name = getNameForAnnotatedObject(descriptor, annotation);
                        name = (name != null) ? name : descriptor.getName();
                        return getEnclosingScope(descriptor).declareUnobfuscatableName(name);
                    }
                    return null;
                }
            };
            Rule<JsName> propertiesCorrespondToSpeciallyTreatedBackingFieldNames = new Rule<JsName>() {
                @Override
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof PropertyDescriptor)) {
                        return null;
                    }
                    NamingScope enclosingScope = getEnclosingScope(descriptor);
                    return enclosingScope.declareObfuscatableName(Namer.getKotlinBackingFieldName(descriptor.getName()));
                }
            };
            //TODO: hack!
            Rule<JsName> toStringHack = new Rule<JsName>() {
                @Override
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof FunctionDescriptor)) {
                        return null;
                    }
                    if (!descriptor.getName().equals("toString")) {
                        return null;
                    }
                    if (((FunctionDescriptor)descriptor).getValueParameters().isEmpty()) {
                        return getEnclosingScope(descriptor).declareUnobfuscatableName("toString");
                    }
                    return null;
                }
            };

            Rule<JsName> overridingDescriptorsReferToOriginalName = new Rule<JsName>() {
                @Override
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    //TODO: refactor
                    if (!(descriptor instanceof FunctionDescriptor)) {
                        return null;
                    }
                    FunctionDescriptor overriddenDescriptor = getOverriddenDescriptor((FunctionDescriptor)descriptor);
                    if (overriddenDescriptor == null) {
                        return null;
                    }
                    return getNameForDescriptor(overriddenDescriptor);
                }
            };
            addRule(namesForStandardClasses);
            addRule(constructorHasTheSameNameAsTheClass);
            addRule(predefinedObjectsHasUnobfuscatableNames);
            addRule(toStringHack);
            addRule(propertiesCorrespondToSpeciallyTreatedBackingFieldNames);
            addRule(namespacesShouldBeDefinedInRootScope);
            addRule(overridingDescriptorsReferToOriginalName);
            addRule(accessorsHasNamesWithSpecialPrefixes);
            addRule(memberDeclarationsInsideParentsScope);
        }
    }

    @NotNull
    private NamingScope getEnclosingScope(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = getContainingDeclaration(descriptor);
        return getScopeForDescriptor(containingDeclaration.getOriginal());
    }


    private final class ScopeGenerator extends Generator<NamingScope> {

        public ScopeGenerator() {
            Rule<NamingScope> generateNewScopesForClassesWithNoAncestors = new Rule<NamingScope>() {
                @Override
                public NamingScope apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof ClassDescriptor)) {
                        return null;
                    }
                    if (getSuperclass((ClassDescriptor)descriptor) == null) {
                        return getRootScope().innerScope("Scope for class " + descriptor.getName());
                    }
                    return null;
                }
            };
            Rule<NamingScope> generateInnerScopesForDerivedClasses = new Rule<NamingScope>() {
                @Override
                public NamingScope apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof ClassDescriptor)) {
                        return null;
                    }
                    ClassDescriptor superclass = getSuperclass((ClassDescriptor)descriptor);
                    if (superclass == null) {
                        return null;
                    }
                    return getScopeForDescriptor(superclass).innerScope("Scope for class " + descriptor.getName());
                }
            };
            Rule<NamingScope> generateNewScopesForNamespaceDescriptors = new Rule<NamingScope>() {
                @Override
                public NamingScope apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof NamespaceDescriptor)) {
                        return null;
                    }
                    return getRootScope().innerScope("Namespace " + descriptor.getName());
                }
            };
            Rule<NamingScope> generateInnerScopesForMembers = new Rule<NamingScope>() {
                @Override
                public NamingScope apply(@NotNull DeclarationDescriptor descriptor) {
                    NamingScope enclosingScope = getEnclosingScope(descriptor);
                    return enclosingScope.innerScope("Scope for member " + descriptor.getName());
                }
            };
            Rule<NamingScope> createFunctionObjectsForCallableDescriptors = new Rule<NamingScope>() {
                @Override
                public NamingScope apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof CallableDescriptor)) {
                        return null;
                    }
                    NamingScope enclosingScope = getEnclosingScope(descriptor);
                    JsFunction correspondingFunction = JsAstUtils.createFunctionWithEmptyBody(enclosingScope.jsScope());
                    NamingScope newScope = enclosingScope.innerScope(correspondingFunction.getScope());
                    assert (!scopeToFunction.containsKey(newScope)) : "Scope to function value overriden for " + descriptor;
                    scopeToFunction.put(newScope, correspondingFunction);
                    return newScope;
                }
            };
            addRule(createFunctionObjectsForCallableDescriptors);
            addRule(generateNewScopesForClassesWithNoAncestors);
            addRule(generateInnerScopesForDerivedClasses);
            addRule(generateNewScopesForNamespaceDescriptors);
            addRule(generateInnerScopesForMembers);
        }
    }

    @Nullable
    public JsNameRef getQualifierForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (qualifierIsNull.get(descriptor.getOriginal()) != null) {
            return null;
        }
        return qualifiers.get(descriptor.getOriginal());
    }

    private final class QualifierGenerator extends Generator<JsNameRef> {
        public QualifierGenerator() {
            Rule<JsNameRef> standardObjectsHaveKotlinQualifier = new Rule<JsNameRef>() {
                @Override
                public JsNameRef apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!standardClasses.isStandardObject(descriptor)) {
                        return null;
                    }
                    return namer.kotlinObject();
                }
            };
            Rule<JsNameRef> namespaceLevelDeclarationsHaveEnclosingNamespacesNamesAsQualifier = new Rule<JsNameRef>() {
                @Override
                public JsNameRef apply(@NotNull DeclarationDescriptor descriptor) {
                    DeclarationDescriptor containingDeclaration = getContainingDeclaration(descriptor);
                    if (!(containingDeclaration instanceof NamespaceDescriptor)) {
                        return null;
                    }
                    JsName containingDeclarationName = getNameForDescriptor(containingDeclaration);
                    JsNameRef qualifier = containingDeclarationName.makeRef();
                    qualifier.setQualifier(getQualifierForDescriptor(containingDeclaration));
                    return qualifier;
                }
            };
            Rule<JsNameRef> constructorHaveTheSameQualifierAsTheClass = new Rule<JsNameRef>() {
                @Override
                public JsNameRef apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof ConstructorDescriptor)) {
                        return null;
                    }
                    ClassDescriptor containingClass = getContainingClass(descriptor);
                    assert containingClass != null : "Can't have constructor without a class";
                    return getQualifierForDescriptor(containingClass);
                }
            };
            Rule<JsNameRef> libraryObjectsHaveKotlinQualifier = new Rule<JsNameRef>() {

                @Override
                public JsNameRef apply(@NotNull DeclarationDescriptor descriptor) {
                    if (isLibraryObject(descriptor)) {
                        return namer.kotlinObject();
                    }
                    return null;
                }
            };
            addRule(libraryObjectsHaveKotlinQualifier);
            addRule(constructorHaveTheSameQualifierAsTheClass);
            addRule(standardObjectsHaveKotlinQualifier);
            addRule(namespaceLevelDeclarationsHaveEnclosingNamespacesNamesAsQualifier);
        }
    }

    private static class QualifierIsNullGenerator extends Generator<Boolean> {

        private QualifierIsNullGenerator() {
            Rule<Boolean> propertiesHaveNoQualifiers = new Rule<Boolean>() {
                @Override
                public Boolean apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof PropertyDescriptor)) {
                        return null;
                    }
                    return true;
                }
            };
            Rule<Boolean> variableAsFunctionsHaveNoQualifiers = new Rule<Boolean>() {
                @Override
                public Boolean apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!isVariableAsFunction(descriptor)) {
                        return null;
                    }
                    return true;
                }
            };
            //TODO: hack!
            Rule<Boolean> nativeObjectsHaveNoQualifiers = new Rule<Boolean>() {
                @Override
                public Boolean apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!AnnotationsUtils.isNativeObject(descriptor)) {
                        return null;
                    }
                    return true;
                }
            };
            Rule<Boolean> topLevelNamespaceHaveNoQualifier = new Rule<Boolean>() {
                @Override
                public Boolean apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof NamespaceDescriptor)) {
                        return null;
                    }
                    if (DescriptorUtils.isTopLevelNamespace((NamespaceDescriptor)descriptor)) {
                        return true;
                    }
                    return null;
                }
            };
            addRule(topLevelNamespaceHaveNoQualifier);
            addRule(propertiesHaveNoQualifiers);
            addRule(variableAsFunctionsHaveNoQualifiers);
            addRule(nativeObjectsHaveNoQualifiers);
        }
    }
}
