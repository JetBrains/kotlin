package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import com.sun.istack.internal.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetParameter;

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
    public JsStatement translateAsFunction(@NotNull JetNamedFunction jetFunction) {
        JsName functionName = translationContext().namespaceScope().declareFreshName(jetFunction.getName());
        JsFunction function = generateFunctionObject(jetFunction);
        return AstUtil.newAssignmentStatement
                (translationContext().getNamespaceQualifiedReference(functionName), function);
    }

    @NotNull
    JsPropertyInitializer translateAsMethod(@NotNull JetNamedFunction jetFunction) {
        JsName functionName = translationContext().namespaceScope().declareFreshName(jetFunction.getName());
        JsFunction function = generateFunctionObject(jetFunction);
        return new JsPropertyInitializer(functionName.makeRef(), function);
    }

    @NotNull
    private JsFunction generateFunctionObject(@NotNull JetNamedFunction jetFunction) {
        JsFunction result = JsFunction.getAnonymousFunctionWithScope
                (translationContext().getScopeForElement(jetFunction));
        List<JsParameter> jsParameters = translateParameters(jetFunction.getValueParameters(), result.getScope());
        JsNode jsBody = translateBody(jetFunction);
        result.setParameters(jsParameters);
        result.setBody(AstUtil.convertToBlock(jsBody));
        return result;
    }

    @NotNull
    private JsNode translateBody(@NotNull JetNamedFunction jetFunction) {
        JetExpression jetBodyExpression = jetFunction.getBodyExpression();
        //TODO support them ffs
        assert jetBodyExpression != null : "Function without body not supported at the moment";
        JsNode body = Translation.translateExpression(jetBodyExpression, translationContext().newFunction(jetFunction));
        if (jetFunction.hasBlockBody()) {
            return AstUtil.convertToBlock(body);
        }
        return AstUtil.convertToBlock(new JsReturn(AstUtil.convertToExpression(body)));
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
