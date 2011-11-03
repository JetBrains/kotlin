package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public class FunctionTranslator extends AbstractTranslator {

    public FunctionTranslator(TranslationContext context) {
        super(context);
    }

    @NotNull
    public JsStatement translateFunction(JetNamedFunction jetFunction) {
        JsNameRef functionName = translationContext().getNamespaceQualifiedReference(getJSName(jetFunction.getName()));
        JsFunction function = generateFunctionObject(jetFunction);
        return AstUtil.convertToStatement(AstUtil.newAssignment(functionName, function));
    }

    private JsFunction generateFunctionObject(JetNamedFunction jetFunction) {
        JetExpression jetBodyExpression = jetFunction.getBodyExpression();
        JsFunction result = new JsFunction(scope());
        JsNode jsBody = (new ExpressionTranslator(functionBodyContext(result)))
                .translate(jetBodyExpression);
        List<JsParameter> jsParameters = translateParameters(jetFunction.getValueParameters());
        result.setParameters(jsParameters);
        result.setBody(AstUtil.convertToBlock(jsBody));
        return result;
    }

    private TranslationContext functionBodyContext(JsFunction function) {
        return translationContext().newFunction(function);
    }

    @NotNull
    private List<JsParameter> translateParameters(List<JetParameter> jetParameters) {
        List<JsParameter> jsParameters = new ArrayList<JsParameter>();
        for (JetParameter jetParameter : jetParameters) {
            jsParameters.add(new JsParameter(getJSName(jetParameter.getName())));
        }
        return jsParameters;
    }
}
