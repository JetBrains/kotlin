package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.ClassSorter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDeclarationsForNamespace;

/**
 * @author Pavel Talanov
 */
//TODO: declaration translator receives NamespaceDescriptor while actually should receive all declarations in namespace
public final class ClassDeclarationTranslator extends AbstractTranslator {

    @NotNull
    private final List<JetDeclaration> namespaceDeclarations;
    @NotNull
    private final Map<JsName, JsName> localToGlobalClassName;
    @NotNull
    private final JsScope dummyFunctionScope;
    @Nullable
    private JsName declarationsObject = null;
    @Nullable
    private JsStatement declarationsStatement = null;

    public ClassDeclarationTranslator(@NotNull TranslationContext context, @NotNull NamespaceDescriptor namespace) {
        super(context);
        this.namespaceDeclarations = getDeclarationsForNamespace(context.bindingContext(), namespace);
        this.localToGlobalClassName = new HashMap<JsName, JsName>();
        this.dummyFunctionScope = new JsScope(context().jsScope(), "class declaration function");
    }

    public void generateDeclarations() {
        declarationsObject = context().jsScope().declareName(Namer.nameForClassesVariable());
        assert declarationsObject != null;
        declarationsStatement =
                AstUtil.newVar(declarationsObject, generateDummyFunctionInvocation());
    }

    @NotNull
    public JsName getDeclarationsObjectName() {
        assert declarationsObject != null : "Should run generateDeclarations first";
        return declarationsObject;
    }

    @NotNull
    public JsStatement getDeclarationsStatement() {
        assert declarationsStatement != null : "Should run generateDeclarations first";
        return declarationsStatement;
    }

    @NotNull
    private JsInvocation generateDummyFunctionInvocation() {
        JsFunction dummyFunction = JsFunction.getAnonymousFunctionWithScope(dummyFunctionScope);
        List<JsStatement> classDeclarations = generateClassDeclarationStatements();
        classDeclarations.add(new JsReturn(generateReturnedObjectLiteral()));
        dummyFunction.setBody(AstUtil.newBlock(classDeclarations));
        return AstUtil.newInvocation(dummyFunction);
    }

    @NotNull
    private JsObjectLiteral generateReturnedObjectLiteral() {
        JsObjectLiteral returnedValueLiteral = new JsObjectLiteral();
        for (JsName localName : localToGlobalClassName.keySet()) {
            returnedValueLiteral.getPropertyInitializers().add(classEntry(localName));
        }
        return returnedValueLiteral;
    }

    @NotNull
    private JsPropertyInitializer classEntry(@NotNull JsName localName) {
        return new JsPropertyInitializer(localToGlobalClassName.get(localName).makeRef(), localName.makeRef());
    }

    @NotNull
    private List<JsStatement> generateClassDeclarationStatements() {
        List<JsStatement> classDeclarations = new ArrayList<JsStatement>();
        for (JetClass jetClass : getClassDeclarations()) {
            classDeclarations.add(generateDeclaration(jetClass));
        }
        removeAliases();
        return classDeclarations;
    }

    private void removeAliases() {
        for (JetClass jetClass : getClassDeclarations()) {
            ClassDescriptor descriptor = BindingUtils.getClassDescriptor(context().bindingContext(), jetClass);
            context().aliaser().removeAliasForDescriptor(descriptor);
        }
    }

    @NotNull
    private List<JetClass> getClassDeclarations() {
        List<JetClass> classes = new ArrayList<JetClass>();
        for (JetDeclaration declaration : namespaceDeclarations) {
            if (declaration instanceof JetClass) {
                classes.add((JetClass) declaration);
            }
        }
        return ClassSorter.sortUsingInheritanceOrder(classes, context().bindingContext());
    }

    @NotNull
    private JsStatement generateDeclaration(@NotNull JetClass declaration) {
        JsName localClassName = generateLocalAlias(declaration);
        JsInvocation classDeclarationExpression =
                Translation.translateClassDeclaration(declaration, context());
        return AstUtil.newVar(localClassName, classDeclarationExpression);
    }

    @NotNull
    private JsName generateLocalAlias(@NotNull JetClass declaration) {
        JsName globalClassName = context().getNameForElement(declaration);
        JsName localAlias = dummyFunctionScope.declareName(globalClassName.getIdent());
        localToGlobalClassName.put(localAlias, globalClassName);
        ClassDescriptor descriptor = BindingUtils.getClassDescriptor(context().bindingContext(), declaration);
        context().aliaser().setAliasForDescriptor(descriptor, localAlias);
        return localAlias;
    }
}
