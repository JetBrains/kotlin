package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import com.sun.istack.internal.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;

import javax.xml.ws.Binding;
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
    public JsStatement translateAsFunction(@NotNull JetNamedFunction jetFunction) {
        JsName functionName = translationContext().namespaceScope().declareFreshName(jetFunction.getName());
        JsFunction function = generateFunctionObject(jetFunction);
        return AstUtil.convertToStatement(AstUtil.newAssignment
                (translationContext().getNamespaceQualifiedReference(functionName), function));
    }

    @NotNull JsPropertyInitializer translateAsMethod(@NotNull JetNamedFunction jetFunction) {
        JsName functionName = translationContext().namespaceScope().declareFreshName(jetFunction.getName());
        JsFunction function = generateFunctionObject(jetFunction);
        return new JsPropertyInitializer(functionName.makeRef(), function);
    }

    @NotNull
    private JsFunction generateFunctionObject(@NotNull JetNamedFunction jetFunction) {
        JetExpression jetBodyExpression = jetFunction.getBodyExpression();
        JsFunction result = JsFunction.getAnonymousFunctionWithScope
                (translationContext().getScopeForElement(jetFunction));
        JsNode jsBody = (new ExpressionTranslator(functionContext(jetFunction)))
                .translate(jetBodyExpression);
        List<JsParameter> jsParameters = translateParameters(jetFunction.getValueParameters(), result.getScope());
        result.setParameters(jsParameters);
        result.setBody(AstUtil.convertToBlock(jsBody));
        return result;
    }

    @NotNull
    private TranslationContext functionContext(@NotNull JetNamedFunction jetFunction) {
        FunctionDescriptor descriptor =
                BindingUtils.getFunctionDescriptor(translationContext().bindingContext(), jetFunction);
        return translationContext().newFunction(descriptor);
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
