package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamespace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Talanov Pavel
 */
//TODO: implement
public final class ClassDeclarationTranslator extends AbstractTranslator {

    @NotNull
    private final JetNamespace namespace;
    @NotNull
    private final Map<JsName, JsName> localToGlobalClassName;
    @NotNull
    private final JsScope dummyFunctionScope;
    @Nullable
    private JsName declarationsObject = null;
    @Nullable
    private JsStatement declarationsStatement = null;

    public ClassDeclarationTranslator(@NotNull TranslationContext context, @NotNull JetNamespace namespace) {
        super(context);
        this.namespace = namespace;
        this.localToGlobalClassName = new HashMap<JsName, JsName>();
        this.dummyFunctionScope = new JsScope(translationContext().enclosingScope(), "class declaration function");
    }

    public void generateDeclarations() {
        //TODO: hardcoded string
        declarationsObject = translationContext().enclosingScope().declareName("classes");
        declarationsStatement =
                AstUtil.newAssignmentStatement(declarationsObject.makeRef(), generateDummyFunctionInvocation());
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
        List<JsStatement> classDeclarations = getClassDeclarationStatements();
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
    private List<JsStatement> getClassDeclarationStatements() {
        List<JsStatement> classDeclarations = new ArrayList<JsStatement>();
        for (JetDeclaration declaration : namespace.getDeclarations()) {
            if (declaration instanceof JetClass) {
                classDeclarations.add(generateDeclaration((JetClass) declaration));
            }
        }
        return classDeclarations;
    }

    @NotNull
    private JsStatement generateDeclaration(@NotNull JetClass declaration) {
        JsName globalClassName = translationContext().getNameForElement(declaration);
        JsName localClassName = dummyFunctionScope.declareName(globalClassName.getIdent());
        localToGlobalClassName.put(localClassName, globalClassName);
        JsInvocation classDeclarationExpression =
                Translation.classTranslator(translationContext()).translateClass(declaration);
        return AstUtil.newVar(localClassName, classDeclarationExpression);
    }
}
