package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.backend.js.ast.JsRootScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.k2js.translate.context.generator.Generator;
import org.jetbrains.k2js.translate.context.generator.Rule;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;

import static org.jetbrains.k2js.translate.utils.AnnotationsUtils.*;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.*;

public class StaticContext {

    public static StaticContext generateStaticContext(@NotNull JetStandardLibrary library,
                                                      @NotNull BindingContext bindingContext) {
        JsProgram program = new JsProgram("main");
        JsRootScope jsRootScope = program.getRootScope();
        Namer namer = Namer.newInstance(jsRootScope);
        Aliaser aliaser = Aliaser.newInstance();
        NamingScope scope = NamingScope.rootScope(jsRootScope);
        Intrinsics intrinsics = Intrinsics.standardLibraryIntrinsics(library);
        StandardClasses standardClasses =
                StandardClasses.bindImplementations(namer.getKotlinScope());
        return new StaticContext(program, bindingContext, aliaser,
                namer, intrinsics, standardClasses, scope);
    }

    @NotNull
    private final JsProgram program;

    @NotNull
    private final BindingContext bindingContext;

    @NotNull
    private final Aliaser aliaser;

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


    //TODO: too many parameters in constructor
    private StaticContext(@NotNull JsProgram program, @NotNull BindingContext bindingContext,
                          @NotNull Aliaser aliaser,
                          @NotNull Namer namer, @NotNull Intrinsics intrinsics,
                          @NotNull StandardClasses standardClasses, @NotNull NamingScope rootScope) {
        this.program = program;
        this.bindingContext = bindingContext;
        this.aliaser = aliaser;
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
    public Aliaser getAliaser() {
        return aliaser;
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
    public StandardClasses getStandardClasses() {
        return standardClasses;
    }

    @NotNull
    public NamingScope getScopeForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        NamingScope namingScope = scopes.get(descriptor);
        assert namingScope != null : "Must have a scope for descriptor";
        return namingScope;
    }

    @Nullable
    public JsNameRef getQualifierForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        //TODO: hack!
        if (AnnotationsUtils.isNativeObject(descriptor)) {
            return null;
        }

        return qualifiers.get(descriptor.getOriginal());
    }

    @NotNull
    public JsName getNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        JsName name = names.get(descriptor.getOriginal());
        assert name != null : "Must have name for descriptor";
        return name;
    }


    private class NameGenerator extends Generator<JsName> {
        public NameGenerator() {
            Rule<JsName> aliasOverridesNames = new Rule<JsName>() {
                @Override
                @Nullable
                public JsName apply(@NotNull DeclarationDescriptor data) {
                    if (aliaser.hasAliasForDeclaration(data)) {
                        return aliaser.getAliasForDeclaration(data);
                    }
                    return null;
                }
            };
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
                    String nameForNamespace = getNameForNamespace((NamespaceDescriptor) descriptor);
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
                    String propertyName = ((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty().getName();
                    String accessorName = Namer.getNameForAccessor(propertyName, isGetter);
                    NamingScope enclosingScope = getEnclosingScope(descriptor);
                    return enclosingScope.declareObfuscatableName(accessorName);
                }
            };

            Rule<JsName> namesAnnotatedAsLibraryHasUnobfuscatableNames = new Rule<JsName>() {
                @Override
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    //TODO: refactor
                    String name = null;
                    AnnotationDescriptor annotation = getAnnotationByName(descriptor, LIBRARY_ANNOTATION_FQNAME);
                    if (annotation != null) {
                        name = AnnotationsUtils.getAnnotationStringParameter(descriptor, LIBRARY_ANNOTATION_FQNAME);
                        name = (!name.isEmpty()) ? name : descriptor.getName();
                    } else {
                        ClassDescriptor containingClass = getContainingClass(descriptor);
                        if (containingClass == null) return null;
                        if (getAnnotationByName(containingClass, LIBRARY_ANNOTATION_FQNAME) != null) {
                            name = descriptor.getName();
                        }
                    }
                    if (name != null) {
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
                    if (((FunctionDescriptor) descriptor).getValueParameters().isEmpty()) {
                        return getEnclosingScope(descriptor).declareUnobfuscatableName("toString");
                    }
                    return null;
                }

            };
            Rule<JsName> namesForNativeObjectsAreUnobfuscatable = new Rule<JsName>() {
                @Override
                public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                    String name = null;
                    AnnotationDescriptor annotation = getAnnotationByName(descriptor, NATIVE_ANNOTATION_FQNAME);
                    if (annotation != null) {
                        name = AnnotationsUtils.getAnnotationStringParameter(descriptor, NATIVE_ANNOTATION_FQNAME);
                        name = (!name.isEmpty()) ? name : descriptor.getName();
                    } else {
                        ClassDescriptor containingClass = getContainingClass(descriptor);
                        if (containingClass == null) return null;
                        if (getAnnotationByName(containingClass, NATIVE_ANNOTATION_FQNAME) != null) {
                            name = descriptor.getName();
                        }
                    }
                    if (name != null) {
                        return getEnclosingScope(descriptor).declareUnobfuscatableName(name);
                    }
                    return null;

                }
            };
            addRule(namesForStandardClasses);
            addRule(aliasOverridesNames);
            addRule(constructorHasTheSameNameAsTheClass);
            addRule(namesAnnotatedAsLibraryHasUnobfuscatableNames);
            addRule(namesForNativeObjectsAreUnobfuscatable);
            addRule(toStringHack);
            addRule(propertiesCorrespondToSpeciallyTreatedBackingFieldNames);
            addRule(namespacesShouldBeDefinedInRootScope);
            addRule(accessorsHasNamesWithSpecialPrefixes);
            addRule(memberDeclarationsInsideParentsScope);
        }
    }

    private NamingScope getEnclosingScope(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = getContainingDeclaration(descriptor);
        return getScopeForDescriptor(containingDeclaration.getOriginal());
    }


    private class ScopeGenerator extends Generator<NamingScope> {

        public ScopeGenerator() {
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
                    return enclosingScope.innerScope("scope for member " + descriptor.getName());
                }
            };
            addRule(generateNewScopesForNamespaceDescriptors);
            addRule(generateInnerScopesForMembers);
        }

    }


    private class QualifierGenerator extends Generator<JsNameRef> {
        public QualifierGenerator() {
            Rule<JsNameRef> namespacesHaveNoQualifiers = new Rule<JsNameRef>() {
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
                    return containingDeclarationName.makeRef();
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

                //TODO: refactor by removing one annotation
                @Override
                public JsNameRef apply(@NotNull DeclarationDescriptor descriptor) {
                    if (getAnnotationByName(descriptor, AnnotationsUtils.LIBRARY_ANNOTATION_FQNAME) != null) {
                        return namer.kotlinObject();
                    }
                    if (getAnnotationByName(descriptor, AnnotationsUtils.LIBRARY_ANNOTATION_FQNAME) != null) {
                        return namer.kotlinObject();
                    }
                    return null;
                }
            };
            Rule<JsNameRef> membersOfAnnotatedClassesHaveKotlinQualifier = new Rule<JsNameRef>() {
                @Override
                public JsNameRef apply(@NotNull DeclarationDescriptor descriptor) {
                    ClassDescriptor containingClass = getContainingClass(descriptor);
                    if (containingClass == null) {
                        return null;
                    }
                    if (getAnnotationByName(descriptor, LIBRARY_ANNOTATION_FQNAME) != null) {
                        return namer.kotlinObject();
                    }
                    return null;
                }
            };
            addRule(libraryObjectsHaveKotlinQualifier);
            addRule(membersOfAnnotatedClassesHaveKotlinQualifier);
            addRule(constructorHaveTheSameQualifierAsTheClass);
            addRule(namespacesHaveNoQualifiers);
            addRule(namespaceLevelDeclarationsHaveEnclosingNamespacesNamesAsQualifier);
        }
    }
}