package k2js.translate;

import com.google.dart.compiler.util.AstUtil;
import com.sun.istack.internal.NotNull;
import compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class FunctionTranslator extends AbstractTranslator {

    public FunctionTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @NotNull
    public JsStatement translateFunction(@NotNull JetNamedFunction jetFunction) {
        JsName functionName = translationContext().namespaceScope().declareFreshName(jetFunction.getName());
        JsFunction function = generateFunctionObject(jetFunction);
        return AstUtil.convertToStatement(AstUtil.newAssignment
                (translationContext().getNamespaceQualifiedReference(functionName), function));
    }

    @NotNull
    private JsFunction generateFunctionObject(@NotNull JetNamedFunction jetFunction) {
        JetExpression jetBodyExpression = jetFunction.getBodyExpression();
        JsFunction result = new JsFunction(scope());
        JsNode jsBody = (new ExpressionTranslator(functionBodyContext(result)))
                .translate(jetBodyExpression);
        List<JsParameter> jsParameters = translateParameters(jetFunction.getValueParameters(), result.getScope());
        result.setParameters(jsParameters);
        result.setBody(AstUtil.convertToBlock(jsBody));
        return result;
    }

    @NotNull
    private TranslationContext functionBodyContext(@NotNull JsFunction function) {
        return translationContext().newFunction(function);
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
