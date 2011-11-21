package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class ClassInitializerVisitor extends AbstractInitializerVisitor {

    @NotNull
    private final JetClass classDeclaration;

    public ClassInitializerVisitor(@NotNull JetClass classDeclaration, @NotNull TranslationContext context) {
        super(context, new JsScope(context.getScopeForElement(classDeclaration),
                "initializer " + classDeclaration.getName()));
        this.classDeclaration = classDeclaration;
    }

    @Override
    @NotNull
    public JsFunction generate() {
        JsFunction result = JsFunction.getAnonymousFunctionWithScope(initializerMethodScope);
        result.setParameters(translatePrimaryConstructorParameters(classDeclaration));
        result.setBody(generateInitializerMethodBody(classDeclaration, initializerMethodContext));
        return result;
    }

    @NotNull
    private JsBlock generateInitializerMethodBody(@NotNull JetClass classDeclaration,
                                                  @NotNull TranslationContext context) {

        List<JsStatement> initializerStatements = generateCallToSuperMethod(classDeclaration, context);
        initializerStatements.addAll(classDeclaration.accept(this, context));
        return AstUtil.newBlock(initializerStatements);
    }

    @NotNull
    private List<JsStatement> generateCallToSuperMethod(@NotNull JetClass classDeclaration,
                                                        @NotNull TranslationContext context) {
        List<JsStatement> result = new ArrayList<JsStatement>();
        if (BindingUtils.hasAncestorClass(context.bindingContext(), classDeclaration)) {
            JsName superMethodName = initializerMethodScope.declareName(Namer.SUPER_METHOD_NAME);
            List<JsExpression> arguments = translateArguments(classDeclaration, context);
            result.add(AstUtil.convertToStatement
                    (AstUtil.newInvocation(AstUtil.thisQualifiedReference(superMethodName), arguments)));
        }
        return result;
    }

    @NotNull
    private List<JsExpression> translateArguments(@NotNull JetClass classDeclaration,
                                                  @NotNull TranslationContext context) {
        JetDelegatorToSuperCall superCall = getSuperCall(classDeclaration);
        return translateArgumentList(superCall.getValueArguments(), context);
    }

    @NotNull
    private JetDelegatorToSuperCall getSuperCall(@NotNull JetClass classDeclaration) {
        JetDelegatorToSuperCall result = null;
        for (JetDelegationSpecifier specifier : classDeclaration.getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorToSuperCall) {
                result = (JetDelegatorToSuperCall) specifier;
            }
        }
        assert result != null : "Class must call ancestor's constructor.";
        return result;
    }


    @Override
    @NotNull
    public List<JsStatement> visitClass(@NotNull JetClass expression, @NotNull TranslationContext context) {
        List<JsStatement> initializerStatements = new ArrayList<JsStatement>();
        for (JetDeclaration declaration : expression.getDeclarations()) {
            initializerStatements.addAll(declaration.accept(this, context));
        }
        return initializerStatements;
    }

    @NotNull
    List<JsParameter> translatePrimaryConstructorParameters(@NotNull JetClass expression) {
        List<JsParameter> result = new ArrayList<JsParameter>();
        List<JetParameter> parameters = expression.getPrimaryConstructorParameters();
        for (JetParameter parameter : parameters) {
            JsName parameterName = initializerMethodScope.declareName(parameter.getName());
            result.add(new JsParameter(parameterName));
        }
        return result;
    }
}
