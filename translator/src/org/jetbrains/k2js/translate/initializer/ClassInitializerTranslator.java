package org.jetbrains.k2js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.psi.JetDelegatorToSuperCall;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.k2js.translate.BindingUtils;
import org.jetbrains.k2js.translate.Namer;
import org.jetbrains.k2js.translate.TranslationContext;
import org.jetbrains.k2js.translate.TranslationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class ClassInitializerTranslator extends AbstractInitializerTranslator {

    @NotNull
    private final JetClass classDeclaration;

    public ClassInitializerTranslator(@NotNull JetClass classDeclaration, @NotNull TranslationContext context) {
        super(new JsScope(context.getScopeForElement(classDeclaration),
                "initializer " + classDeclaration.getName()), context);
        this.classDeclaration = classDeclaration;
    }

    @Override
    @NotNull
    protected JsFunction generateInitializerFunction() {
        JsFunction result = JsFunction.getAnonymousFunctionWithScope(initializerMethodScope);
        result.setParameters(translatePrimaryConstructorParameters());
        result.setBody(generateInitializerMethodBody());
        return result;
    }

    @NotNull
    private JsBlock generateInitializerMethodBody() {

        List<JsStatement> initializerStatements = generateCallToSuperMethod();
        //   initializerStatements.addAll(initializersForParameterProperties());
        initializerStatements.addAll(translatePropertyAndAnonymousInitializers(classDeclaration));
        return AstUtil.newBlock(initializerStatements);
    }

    @NotNull
    private List<JsStatement> initializersForParameterProperties() {
        return null;
    }

    @NotNull
    private List<JsStatement> generateCallToSuperMethod() {
        List<JsStatement> result = new ArrayList<JsStatement>();
        if (BindingUtils.hasAncestorClass(translationContext().bindingContext(), classDeclaration)) {
            JsName superMethodName = initializerMethodScope.declareName(Namer.SUPER_METHOD_NAME);
            List<JsExpression> arguments = translateArguments();
            result.add(AstUtil.convertToStatement
                    (AstUtil.newInvocation(AstUtil.thisQualifiedReference(superMethodName), arguments)));
        }
        return result;
    }

    @NotNull
    private List<JsExpression> translateArguments() {
        JetDelegatorToSuperCall superCall = getSuperCall();
        return TranslationUtils.translateArgumentList(superCall.getValueArguments(), translationContext());
    }

    @NotNull
    private JetDelegatorToSuperCall getSuperCall() {
        JetDelegatorToSuperCall result = null;
        for (JetDelegationSpecifier specifier : classDeclaration.getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorToSuperCall) {
                result = (JetDelegatorToSuperCall) specifier;
            }
        }
        assert result != null : "Class must call ancestor's constructor.";
        return result;
    }

    @NotNull
    List<JsParameter> translatePrimaryConstructorParameters() {
        List<JsParameter> result = new ArrayList<JsParameter>();
        List<JetParameter> parameters = classDeclaration.getPrimaryConstructorParameters();
        for (JetParameter parameter : parameters) {
            JsName parameterName = initializerMethodScope.declareName(parameter.getName());
            result.add(new JsParameter(parameterName));
        }
        return result;
    }
}
