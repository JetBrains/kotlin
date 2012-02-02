package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyAccessorDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.translate.context.declaration.DeclarationFacade;
import org.jetbrains.k2js.translate.context.generator.Generator;
import org.jetbrains.k2js.translate.context.generator.Rule;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getContainingDeclaration;

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
        //TODO: chained call on static context suspicious
        NamingScope declarationScope = staticContext.getDeclarationFacade().getKotlinDeclarations().getScope(descriptor);
        return contextWithScope(declarationScope);
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
        return scopes.get(descriptor);
    }

    @NotNull
    public NamingScope getScopeForElement(@NotNull JetElement element) {
        DeclarationDescriptor descriptorForElement = BindingUtils.getDescriptorForElement(bindingContext(), element);
        return getScopeForDescriptor(descriptorForElement);
    }

    @NotNull
    public JsNameRef getQualifierForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (standardClasses().isStandardObject(descriptor)) {
            return namer().kotlinObject();
        }
        return staticContext.getQualifier(descriptor);
    }


    public boolean hasQualifierForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return (staticContext.hasQualifier(descriptor) ||
                standardClasses().isStandardObject(descriptor));
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
    public JsName declareLocalVariable(@NotNull DeclarationDescriptor descriptor) {
        return dynamicContext.declareLocalVariable(descriptor);
    }

    @NotNull
    public JsName declareLocalVariable(@NotNull JetElement element) {
        DeclarationDescriptor declarationDescriptor =
                BindingUtils.getDescriptorForElement(bindingContext(), element);
        return dynamicContext.declareLocalVariable(declarationDescriptor);
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
    public DeclarationFacade declarationFacade() {
        return staticContext.getDeclarationFacade();
    }

    @NotNull
    public JsName getNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return names.get(descriptor);
    }

    @NotNull
    private Generator<JsName> nameGenerator() {
        Generator<JsName> nameGenerator = new Generator<JsName>();
        nameGenerator.addRule(new Rule<JsName>() {
            @Override
            @Nullable
            public JsName apply(@NotNull DeclarationDescriptor data) {
                if (aliaser().hasAliasForDeclaration(data)) {
                    return aliaser().getAliasForDeclaration(data);
                }
                return null;
            }
        });
        nameGenerator.addRule(new Rule<JsName>() {
            @Override
            @Nullable
            public JsName apply(@NotNull DeclarationDescriptor data) {
                if (!standardClasses().isStandardObject(data)) {
                    return null;
                }
                return standardClasses().getStandardObjectName(data);
            }
        });
        nameGenerator.addRule(new Rule<JsName>() {
            @Override
            @Nullable
            public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                if (dynamicContext.isDeclared(descriptor)) {
                    return dynamicContext.getLocalName(descriptor);
                }
                return null;
            }
        });
        nameGenerator.addRule(new Rule<JsName>() {
            @Override
            @Nullable
            public JsName apply(@NotNull DeclarationDescriptor descriptor) {
                if (staticContext.isDeclared(descriptor)) {
                    return staticContext.getGlobalName(descriptor);
                }
                return null;
            }
        });
        return nameGenerator;
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
        Rule<NamingScope> generateInnerScopesForFunctions = new Rule<NamingScope>() {
            @Override
            public NamingScope apply(@NotNull DeclarationDescriptor descriptor) {
//                if (!(descriptor instanceof FunctionDescriptor)) {
//                    return null;
//                }
                DeclarationDescriptor containingDeclaration = getContainingDeclaration(descriptor);
                NamingScope enclosingScope = getScopeForDescriptor(containingDeclaration);
                return enclosingScope.innerScope("scope for member " + descriptor.getName());
            }
        };
        scopeGenerator.addRule(generateNewScopesForNamespaceDescriptors);
        scopeGenerator.addRule(generateInnerScopesForFunctions);

        return scopeGenerator;
    }

}
