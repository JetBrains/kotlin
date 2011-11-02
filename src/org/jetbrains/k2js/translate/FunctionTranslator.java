package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.backend.js.ast.JsNode;
import com.google.dart.compiler.backend.js.ast.JsParameter;
import com.google.dart.compiler.backend.js.ast.JsScope;
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
    public JsFunction translateFunction(JetNamedFunction jetFunction) {
        String name = jetFunction.getName();
        JetExpression jetBodyExpression = jetFunction.getBodyExpression();
        JsFunction result = new JsFunction(scope(), getJSName(name));
        JsNode jsBody = (new ExpressionTranslator(functionBodyContext(result.getScope())))
                .translate(jetBodyExpression);
        List<JsParameter> jsParameters = translateParameters(jetFunction.getValueParameters());
        result.setParameters(jsParameters);
        result.setBody(AstUtil.convertToBlock(jsBody));
        return result;
    }

    private TranslationContext functionBodyContext(JsScope functionScope) {
        return translationContext().newType(ContextType.FUNCTION_BODY).newScope(functionScope);
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
