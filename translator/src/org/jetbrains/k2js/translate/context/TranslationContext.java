package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.translate.context.declaration.AnnotationsUtils;
import org.jetbrains.k2js.translate.context.generator.Generator;
import org.jetbrains.k2js.translate.context.generator.Rule;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import static org.jetbrains.k2js.translate.context.declaration.AnnotationsUtils.*;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.*;

/**
 * @author Pavel Talanov
 */
public final class TranslationContext {

    @NotNull
    private final DynamicContext dynamicContext;
    @NotNull
    private final StaticContext staticContext;
    @NotNull
    private final Generator<JsName> names = nameGenerator();
    @NotNull
    private final Generator<NamingScope> scopes = scopeGenerator();
    @NotNull
    private final Generator<JsNameRef> qualifiers = qualifierGenerator();

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
    public TranslationContext contextWithScope(@NotNull NamingScope newScope) {
        return new TranslationContext(staticContext, DynamicContext.contextWithScope(newScope));
    }

    // Note: Should be used ONLY if scope has no corresponding descriptor
    @NotNull
    public TranslationContext innerJsScope(@NotNull JsScope enclosingScope) {
        return new TranslationContext(staticContext, dynamicContext.innerScope(enclosingScope));
    }

    @NotNull
    public TranslationContext innerBlock(@NotNull JsBlock block) {
        return new TranslationContext(staticContext, dynamicContext.innerBlock(block));
    }

    @NotNull
    public TranslationContext newNamespace(@NotNull NamespaceDescriptor namespace) {
        return newDeclaration(namespace);
    }

    @NotNull
    public TranslationContext newDeclaration(@NotNull DeclarationDescriptor descriptor) {
        return contextWithScope(getScopeForDescriptor(descriptor));
    }

    @NotNull
    public TranslationContext newClass(@NotNull JetClass declaration) {
        return newDeclaration(BindingUtils.getClassDescriptor(staticContext.getBindingContext(), declaration));
    }

    @NotNull
    public TranslationContext newPropertyAccess(@NotNull JetPropertyAccessor declaration) {
        return newDeclaration(BindingUtils.getPropertyAccessorDescriptor(staticContext.getBindingContext(), declaration));
    }

    @NotNull
    public TranslationContext newPropertyAccess(@NotNull PropertyAccessorDescriptor descriptor) {
        return newDeclaration(descriptor);
    }

    @NotNull
    public TranslationContext newFunctionDeclaration(@NotNull JetNamedFunction declaration) {
        return newDeclaration(BindingUtils.getFunctionDescriptor(staticContext.getBindingContext(), declaration));
    }

    @NotNull
    public BindingContext bindingContext() {
        return staticContext.getBindingContext();
    }


    @NotNull
    public NamingScope getScopeForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        NamingScope namingScope = scopes.get(descriptor);
        assert namingScope != null : "Must have a scope for descriptor";
        return namingScope;
    }

    @NotNull
    public NamingScope getScopeForElement(@NotNull JetElement element) {
        DeclarationDescriptor descriptorForElement = BindingUtils.getDescriptorForElement(bindingContext(), element);
        return getScopeForDescriptor(descriptorForElement);
    }

    @NotNull
    public JsName getNameForElement(@NotNull JetElement element) {
        DeclarationDescriptor descriptor = BindingUtils.getDescriptorForElement(bindingContext(), element);
        return getNameForDescriptor(descriptor);
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
    private StandardClasses standardClasses() {
        return staticContext.getStandardClasses();
    }

    @NotNull
    public JsScope jsScope() {
        return dynamicContext.jsScope();
    }

    @NotNull
    public JsBlock jsBlock() {
        return dynamicContext.jsBlock();
    }

    @NotNull
    public JsName getNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        JsName name = names.get(descriptor.getOriginal());
        assert name != null : "Must have name for descriptor";
        return name;
    }

    @NotNull
    private Generator<JsName> nameGenerator() {

        Rule<JsName> aliasOverridesNames = new Rule<JsName>() {
            @Override
            @Nullable
            public JsName apply(@NotNull DeclarationDescriptor data) {
                if (aliaser().hasAliasForDeclaration(data)) {
                    return aliaser().getAliasForDeclaration(data);
                }
                return null;
            }
        };
        Rule<JsName> namesForStandardClasses = new Rule<JsName>() {
            @Override
            @Nullable
            public JsName apply(@NotNull DeclarationDescriptor data) {
                if (!standardClasses().isStandardObject(data)) {
                    return null;
                }
                return standardClasses().getStandardObjectName(data);
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
                return staticContext.getRootScope().declareUnobfuscatableName(nameForNamespace);
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

        Rule<JsName> namesAnnotatedAsStandard = new Rule<JsName>() {
            @Override
            public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                //TODO: refactor
                String name = null;
                AnnotationDescriptor annotation = getAnnotationByName(descriptor, LIBRARY_FUNCTION_ANNOTATION_FQNAME);
                if (annotation != null) {
                    name = AnnotationsUtils.annotationStringParameter(descriptor, LIBRARY_FUNCTION_ANNOTATION_FQNAME);
                    name = (!name.isEmpty()) ? name : descriptor.getName();
                } else {
                    ClassDescriptor containingClass = getContainingClass(descriptor);
                    if (containingClass == null) return null;
                    if (getAnnotationByName(containingClass, LIBRARY_CLASS_ANNOTATION_FQNAME) != null) {
                        name = descriptor.getName();
                    }
                }
                if (name != null) {
                    return getEnclosingScope(descriptor).declareUnobfuscatableName(name);
                }
                return null;
            }
        };
        Generator<JsName> nameGenerator = new Generator<JsName>();
        nameGenerator.addRule(namesForStandardClasses);
        nameGenerator.addRule(aliasOverridesNames);
        nameGenerator.addRule(constructorHasTheSameNameAsTheClass);
        nameGenerator.addRule(namesAnnotatedAsStandard);
        nameGenerator.addRule(namespacesShouldBeDefinedInRootScope);
        nameGenerator.addRule(accessorsHasNamesWithSpecialPrefixes);
        nameGenerator.addRule(memberDeclarationsInsideParentsScope);
        return nameGenerator;
    }

    private NamingScope getEnclosingScope(DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = getContainingDeclaration(descriptor);
        return getScopeForDescriptor(containingDeclaration.getOriginal());
    }

    @NotNull
    private Generator<NamingScope> scopeGenerator() {
        Generator<NamingScope> scopeGenerator = new Generator<NamingScope>();
        Rule<NamingScope> generateNewScopesForNamespaceDescriptors = new Rule<NamingScope>() {
            @Override
            public NamingScope apply(@NotNull DeclarationDescriptor descriptor) {
                if (!(descriptor instanceof NamespaceDescriptor)) {
                    return null;
                }
                return staticContext.getRootScope().innerScope("Namespace " + descriptor.getName());
            }
        };
        Rule<NamingScope> generateInnerScopesForMembers = new Rule<NamingScope>() {
            @Override
            public NamingScope apply(@NotNull DeclarationDescriptor descriptor) {
                NamingScope enclosingScope = getEnclosingScope(descriptor);
                return enclosingScope.innerScope("scope for member " + descriptor.getName());
            }
        };
        scopeGenerator.addRule(generateNewScopesForNamespaceDescriptors);
        scopeGenerator.addRule(generateInnerScopesForMembers);

        return scopeGenerator;
    }

    @Nullable
    public JsNameRef getQualifierForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return qualifiers.get(descriptor.getOriginal());
    }

    @NotNull
    private Generator<JsNameRef> qualifierGenerator() {
        Generator<JsNameRef> qualifierGenerator = new Generator<JsNameRef>();
        Rule<JsNameRef> namespacesHaveNoQualifiers = new Rule<JsNameRef>() {
            @Override
            public JsNameRef apply(@NotNull DeclarationDescriptor descriptor) {
                if (!standardClasses().isStandardObject(descriptor)) {
                    return null;
                }
                return namer().kotlinObject();
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
        Rule<JsNameRef> annonatedObjectsHaveKotlinQualifier = new Rule<JsNameRef>() {

            //TODO: refactor by removing one annotation
            @Override
            public JsNameRef apply(@NotNull DeclarationDescriptor descriptor) {
                if (getAnnotationByName(descriptor, AnnotationsUtils.LIBRARY_FUNCTION_ANNOTATION_FQNAME) != null) {
                    return namer().kotlinObject();
                }
                if (getAnnotationByName(descriptor, AnnotationsUtils.LIBRARY_CLASS_ANNOTATION_FQNAME) != null) {
                    return namer().kotlinObject();
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
                if (getAnnotationByName(descriptor, LIBRARY_CLASS_ANNOTATION_FQNAME) != null) {
                    return namer().kotlinObject();
                }
                return null;
            }
        };
        qualifierGenerator.addRule(annonatedObjectsHaveKotlinQualifier);
        qualifierGenerator.addRule(membersOfAnnotatedClassesHaveKotlinQualifier);
        qualifierGenerator.addRule(constructorHaveTheSameQualifierAsTheClass);
        qualifierGenerator.addRule(namespacesHaveNoQualifiers);
        qualifierGenerator.addRule(namespaceLevelDeclarationsHaveEnclosingNamespacesNamesAsQualifier);
        return qualifierGenerator;
    }

}
