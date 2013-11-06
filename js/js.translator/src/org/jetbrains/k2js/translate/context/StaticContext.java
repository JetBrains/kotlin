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

import com.google.common.collect.Maps;
import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.config.LibrarySourcesConfig;
import org.jetbrains.k2js.translate.context.generator.Generator;
import org.jetbrains.k2js.translate.context.generator.Rule;
import org.jetbrains.k2js.translate.expression.LiteralFunctionTranslator;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import static org.jetbrains.k2js.translate.utils.AnnotationsUtils.getNameForAnnotatedObject;
import static org.jetbrains.k2js.translate.utils.AnnotationsUtils.isLibraryObject;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.*;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.getMangledName;

/**
 * Aggregates all the static parts of the context.
 */
public final class StaticContext {

    public static StaticContext generateStaticContext(@NotNull BindingContext bindingContext, @NotNull EcmaVersion ecmaVersion) {
        JsProgram program = new JsProgram("main");
        Namer namer = Namer.newInstance(program.getRootScope());
        Intrinsics intrinsics = new Intrinsics();
        StandardClasses standardClasses = StandardClasses.bindImplementations(namer.getKotlinScope());
        return new StaticContext(program, bindingContext, namer, intrinsics, standardClasses, program.getRootScope(), ecmaVersion);
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
    private final JsScope rootScope;

    @NotNull
    private final Generator<JsName> names = new NameGenerator();
    @NotNull
    private final Generator<JsScope> scopes = new ScopeGenerator();
    @NotNull
    private final Generator<JsNameRef> qualifiers = new QualifierGenerator();
    @NotNull
    private final Generator<Boolean> qualifierIsNull = new QualifierIsNullGenerator();

    @NotNull
    private final Map<JsScope, JsFunction> scopeToFunction = Maps.newHashMap();

    @NotNull
    private final EcmaVersion ecmaVersion;

    @NotNull
    private LiteralFunctionTranslator literalFunctionTranslator;

    //TODO: too many parameters in constructor
    private StaticContext(@NotNull JsProgram program, @NotNull BindingContext bindingContext,
            @NotNull Namer namer, @NotNull Intrinsics intrinsics,
            @NotNull StandardClasses standardClasses, @NotNull JsScope rootScope, @NotNull EcmaVersion ecmaVersion) {
        this.program = program;
        this.bindingContext = bindingContext;
        this.namer = namer;
        this.intrinsics = intrinsics;
        this.rootScope = rootScope;
        this.standardClasses = standardClasses;
        this.ecmaVersion = ecmaVersion;
    }

    public void initTranslators(TranslationContext programContext) {
        literalFunctionTranslator = new LiteralFunctionTranslator(programContext);
    }

    @NotNull
    public LiteralFunctionTranslator getLiteralFunctionTranslator() {
        return literalFunctionTranslator;
    }

    public boolean isEcma5() {
        return ecmaVersion == EcmaVersion.v5;
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
    public JsScope getRootScope() {
        return rootScope;
    }

    @NotNull
    public JsScope getScopeForDescriptor(@NotNull DeclarationDescriptor descriptor) {
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
        return new JsNameRef(getNameForDescriptor(descriptor), getQualifierForDescriptor(descriptor));
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

                    String name = Namer.generateNamespaceName(descriptor);
                    return getRootScope().declareName(name);
                }
            };
            Rule<JsName> memberDeclarationsInsideParentsScope = new Rule<JsName>() {
                @Override
                @Nullable
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    JsScope scope = getEnclosingScope(descriptor);
                    DeclarationDescriptor declaration = descriptor.getContainingDeclaration();
                    if (!(descriptor instanceof FunctionDescriptor) || !(declaration instanceof ClassDescriptor)) {
                        return scope.declareFreshName(descriptor.getName().asString());
                    }

                    Collection<FunctionDescriptor> functions =
                            ((ClassDescriptor) declaration).getDefaultType().getMemberScope().getFunctions(descriptor.getName());
                    String name = descriptor.getName().asString();
                    int counter = -1;
                    if (functions.size() > 1) {
                        // see testOverloadedFun
                        FunctionDescriptor[] sorted = functions.toArray(new FunctionDescriptor[functions.size()]);
                        Arrays.sort(sorted, new Comparator<FunctionDescriptor>() {
                            @Override
                            public int compare(@NotNull FunctionDescriptor a, @NotNull FunctionDescriptor b) {
                                Integer result = Visibilities.compare(b.getVisibility(), a.getVisibility());
                                if (result == null) {
                                    return 0;
                                }
                                else if (result == 0) {
                                    // open fun > not open fun
                                    int aWeight = a.getModality().isOverridable() ? 1 : 0;
                                    int bWeight = b.getModality().isOverridable() ? 1 : 0;
                                    return bWeight - aWeight;
                                }

                                return result;
                            }
                        });
                        for (FunctionDescriptor function : sorted) {
                            if (function == descriptor) {
                                break;
                            }
                            counter++;
                        }
                    }

                    return scope.declareName(counter == -1 ? name : name + '_' + counter);
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

            // ecma 5 property name never declares as obfuscatable:
            // 1) property cannot be overloaded, so, name collision is not possible
            // 2) main reason: if property doesn't have any custom accessor, value holder will have the same name as accessor, so, the same name will be declared more than once
            //
            // But extension property may obfuscatable, because transform into function. Example: String.foo = 1, Int.foo = 2
            Rule<JsName> propertyOrPropertyAccessor = new Rule<JsName>() {
                @Override
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    PropertyDescriptor propertyDescriptor;
                    if (descriptor instanceof PropertyAccessorDescriptor) {
                        propertyDescriptor = ((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty();
                    }
                    else if (descriptor instanceof PropertyDescriptor) {
                        propertyDescriptor = (PropertyDescriptor) descriptor;
                    }
                    else {
                        return null;
                    }

                    String nameFromAnnotation = getNameForAnnotatedObject(propertyDescriptor);
                    if (nameFromAnnotation != null) {
                        return declarePropertyOrPropertyAccessorName(descriptor, nameFromAnnotation, false);
                    }

                    String propertyName =  propertyDescriptor.getName().asString();

                    if (!isExtension(propertyDescriptor)) {
                        if (propertyDescriptor.getVisibility() == Visibilities.PRIVATE) {
                            propertyName = getMangledName(propertyDescriptor, propertyName);
                        }
                        return declarePropertyOrPropertyAccessorName(descriptor, propertyName, false);
                    } else {
                        if (descriptor instanceof PropertyDescriptor) {
                            return declarePropertyOrPropertyAccessorName(descriptor, propertyName, true);
                        } else {
                            String propertyJsName = getNameForDescriptor(propertyDescriptor).getIdent();
                            boolean isGetter = descriptor instanceof PropertyGetterDescriptor;
                            String accessorName = Namer.getNameForAccessor(propertyJsName, isGetter, false);
                            return declarePropertyOrPropertyAccessorName(descriptor, accessorName, false);
                        }
                    }
                }
            };

            Rule<JsName> predefinedObjectsHasUnobfuscatableNames = new Rule<JsName>() {
                @Override
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    String name = getNameForAnnotatedObject(descriptor);
                    if (name != null) return getEnclosingScope(descriptor).declareName(name);
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
                    FunctionDescriptor overriddenDescriptor = getOverriddenDescriptor((FunctionDescriptor) descriptor);
                    if (overriddenDescriptor == null) {
                        return null;
                    }

                    JsScope scope = getEnclosingScope(descriptor);
                    JsName result = getNameForDescriptor(overriddenDescriptor);
                    scope.declareName(result.getIdent());
                    return result;
                }
            };
            addRule(namesForStandardClasses);
            addRule(constructorHasTheSameNameAsTheClass);
            addRule(propertyOrPropertyAccessor);
            addRule(predefinedObjectsHasUnobfuscatableNames);
            addRule(namespacesShouldBeDefinedInRootScope);
            addRule(overridingDescriptorsReferToOriginalName);
            addRule(memberDeclarationsInsideParentsScope);
        }
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
                        return getRootScope().innerScope("Scope for class " + descriptor.getName());
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
                    return getScopeForDescriptor(superclass).innerScope("Scope for class " + descriptor.getName());
                }
            };
            Rule<JsScope> generateNewScopesForNamespaceDescriptors = new Rule<JsScope>() {
                @Override
                public JsScope apply(@NotNull DeclarationDescriptor descriptor) {
                    if (!(descriptor instanceof NamespaceDescriptor)) {
                        return null;
                    }
                    return getRootScope().innerScope("Namespace " + descriptor.getName());
                }
            };
            //TODO: never get there
            Rule<JsScope> generateInnerScopesForMembers = new Rule<JsScope>() {
                @Override
                public JsScope apply(@NotNull DeclarationDescriptor descriptor) {
                    JsScope enclosingScope = getEnclosingScope(descriptor);
                    return enclosingScope.innerScope("Scope for member " + descriptor.getName());
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
            //TODO: review and refactor
            Rule<JsNameRef> namespaceLevelDeclarationsHaveEnclosingNamespacesNamesAsQualifier = new Rule<JsNameRef>() {
                @Override
                public JsNameRef apply(@NotNull DeclarationDescriptor descriptor) {
                    DeclarationDescriptor containingDescriptor = getContainingDeclaration(descriptor);
                    if (!(containingDescriptor instanceof NamespaceDescriptor)) {
                        return null;
                    }

                    JsNameRef result = new JsNameRef(getNameForDescriptor(containingDescriptor));
                    if (DescriptorUtils.isRootNamespace((NamespaceDescriptor) containingDescriptor)) {
                        return result;
                    }

                    JsNameRef qualifier = result;
                    while ((containingDescriptor = getContainingDeclaration(containingDescriptor)) instanceof NamespaceDescriptor &&
                           !DescriptorUtils.isRootNamespace((NamespaceDescriptor) containingDescriptor)) {
                        JsNameRef ref = getNameForDescriptor(containingDescriptor).makeRef();
                        qualifier.setQualifier(ref);
                        qualifier = ref;
                    }

                    PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
                    if (element == null && descriptor instanceof PropertyAccessorDescriptor) {
                        element = BindingContextUtils.descriptorToDeclaration(bindingContext, ((PropertyAccessorDescriptor) descriptor)
                                .getCorrespondingProperty());
                    }

                    if (element != null) {
                        PsiFile file = element.getContainingFile();
                        String moduleName = file.getUserData(LibrarySourcesConfig.EXTERNAL_MODULE_NAME);
                        if (LibrarySourcesConfig.UNKNOWN_EXTERNAL_MODULE_NAME.equals(moduleName)) {
                            return null;
                        }
                        else if (moduleName != null) {
                            qualifier.setQualifier(new JsArrayAccess(namer.kotlin("modules"), program.getStringLiteral(moduleName)));
                        }
                    }

                    if (qualifier.getQualifier() == null) {
                        qualifier.setQualifier(new JsNameRef(Namer.getRootNamespaceName()));
                    }

                    return result;
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
                    if (descriptor instanceof NamespaceDescriptor && DescriptorUtils.isRootNamespace((NamespaceDescriptor) descriptor)) {
                        return true;
                    }
                    return null;
                }
            };
            addRule(topLevelNamespaceHaveNoQualifier);
            addRule(propertiesHaveNoQualifiers);
            addRule(nativeObjectsHaveNoQualifiers);
        }
    }
}
