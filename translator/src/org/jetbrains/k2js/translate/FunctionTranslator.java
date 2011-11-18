package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import com.sun.istack.internal.NotNull;
import org.jetbrains.jet.lang.psi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class FunctionTranslator extends AbstractTranslator {

    @NotNull
    public static FunctionTranslator newInstance(@NotNull TranslationContext context) {
        return new FunctionTranslator(context);
    }

    private FunctionTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @NotNull
    public JsStatement translateAsFunctionDeclaration(@NotNull JetNamedFunction expression) {
        JsName functionName = translationContext().getNameForElement(expression);
        JsFunction function = generateFunctionObject(expression);
        return AstUtil.newAssignmentStatement
                (translationContext().getNamespaceQualifiedReference(functionName), function);
    }

    @NotNull
    JsPropertyInitializer translateAsMethod(@NotNull JetNamedFunction expression) {
        JsName functionName = translationContext().getNameForElement(expression);
        JsFunction function = generateFunctionObject(expression);
        return new JsPropertyInitializer(functionName.makeRef(), function);
    }

    @NotNull
    JsFunction translateAsLiteral(@NotNull JetFunctionLiteral expression) {
        return generateFunctionObject(expression);
    }

    @NotNull
    private JsFunction generateFunctionObject(@NotNull JetFunction jetFunction) {
        JsFunction result = createFunctionObject(jetFunction);
        List<JsParameter> jsParameters = translateParameters(jetFunction.getValueParameters(), result.getScope());
        JsNode jsBody = translateBody(jetFunction, result.getScope());
        result.setParameters(jsParameters);
        result.setBody(AstUtil.convertToBlock(jsBody));
        return result;
    }

    private JsFunction createFunctionObject(JetFunction function) {
        if (function instanceof JetNamedFunction) {
            return JsFunction.getAnonymousFunctionWithScope
                    (translationContext().getScopeForElement(function));
        }
        if (function instanceof JetFunctionLiteral) {
            return new JsFunction(translationContext().enclosingScope());
        }
        throw new AssertionError("Unsupported type of function.");
    }

    @NotNull
    private JsStatement translateBody(@NotNull JetFunction function, @NotNull JsScope functionScope) {
        JetExpression jetBodyExpression = function.getBodyExpression();
        //TODO decide if there are cases where this assert is illegal
        assert jetBodyExpression != null : "Function without body not supported";
        JsNode body = Translation.translateExpression(jetBodyExpression, functionBodyContext(function, functionScope));
        if (function.hasBlockBody()) {
            return AstUtil.convertToBlock(body);
        }
        return AstUtil.convertToBlock(new JsReturn(AstUtil.convertToExpression(body)));
    }

    private TranslationContext functionBodyContext(JetFunction function, JsScope functionScope) {
        if (function instanceof JetNamedFunction) {
            return translationContext().newFunctionDeclaration((JetNamedFunction) function);
        }
        if (function instanceof JetFunctionLiteral) {
            return translationContext().newFunctionLiteral(functionScope);
        }
        throw new AssertionError("Unsupported type of function.");
    }

    @NotNull
    private List<JsParameter> translateParameters(@NotNull List<JetParameter> jetParameters,
                                                  @NotNull JsScope functionScope) {
        List<JsParameter> jsParameters = new ArrayList<JsParameter>();
        for (JetParameter jetParameter : jetParameters) {
            JsName parameterName = functionScope.declareName(jetParameter.getName());
            jsParameters.add(new JsParameter(parameterName));
        }
        return jsParameters;
    }
}
