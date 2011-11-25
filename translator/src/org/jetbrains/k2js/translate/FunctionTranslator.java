package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetFunctionLiteral;
import org.jetbrains.jet.lang.psi.JetNamedFunction;

import java.util.List;

/**
 * @author Talanov Pavel
 */
//TODO: think about adding state to translator
public final class FunctionTranslator extends AbstractTranslator {

    @NotNull
    public static FunctionTranslator newInstance(@NotNull TranslationContext context) {
        return new FunctionTranslator(context);
    }

    private FunctionTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @NotNull
    public JsPropertyInitializer translateAsMethod(@NotNull JetNamedFunction expression) {
        JsName functionName = context().getNameForElement(expression);
        JsFunction function = generateFunctionObject(expression);
        return new JsPropertyInitializer(functionName.makeRef(), function);
    }

    @NotNull
    public JsFunction translateAsLiteral(@NotNull JetFunctionLiteral expression) {
        return generateFunctionObject(expression);
    }

    @NotNull
    private JsFunction generateFunctionObject(@NotNull JetFunction jetFunction) {
        JsFunction result = createFunctionObject(jetFunction);
        result.setParameters(TranslationUtils.translateParameters
                (jetFunction.getValueParameters(), result.getScope()));
        result.setBody(translateBody(jetFunction, result.getScope()));
        return result;
    }

    private JsFunction createFunctionObject(JetFunction function) {
        if (function instanceof JetNamedFunction) {
            return JsFunction.getAnonymousFunctionWithScope
                    (context().getScopeForElement(function));
        }
        if (function instanceof JetFunctionLiteral) {
            return new JsFunction(context().enclosingScope());
        }
        throw new AssertionError("Unsupported type of function.");
    }

    @NotNull
    private JsBlock translateBody(@NotNull JetFunction function, @NotNull JsScope functionScope) {
        JetExpression jetBodyExpression = function.getBodyExpression();
        //TODO decide if there are cases where this assert is illegal
        assert jetBodyExpression != null : "Function without body not supported";
        JsNode body = Translation.translateExpression(jetBodyExpression, functionBodyContext(function, functionScope));
        return wrapWithReturnIfNeeded(body, !function.hasBlockBody());
    }

    @NotNull
    private JsBlock wrapWithReturnIfNeeded(@NotNull JsNode body, boolean needsReturn) {
        if (!needsReturn) {
            return AstUtil.convertToBlock(body);
        }
        if (body instanceof JsExpression) {
            return AstUtil.convertToBlock(new JsReturn(AstUtil.convertToExpression(body)));
        }
        if (body instanceof JsBlock) {
            JsBlock bodyBlock = (JsBlock) body;
            addReturnToBlockStatement(bodyBlock);
            return bodyBlock;
        }
        throw new AssertionError("Invalid node as function body");
    }

    private void addReturnToBlockStatement(@NotNull JsBlock bodyBlock) {
        List<JsStatement> statements = bodyBlock.getStatements();
        int lastIndex = statements.size() - 1;
        JsStatement lastStatement = statements.get(lastIndex);
        statements.set(lastIndex,
                new JsReturn(AstUtil.extractExpressionFromStatement(lastStatement)));
    }

    @NotNull
    private TranslationContext functionBodyContext(@NotNull JetFunction function,
                                                   @NotNull JsScope functionScope) {
        if (function instanceof JetNamedFunction) {
            return context().newFunctionDeclaration((JetNamedFunction) function);
        }
        if (function instanceof JetFunctionLiteral) {
            return context().newFunctionLiteral(functionScope);
        }
        throw new AssertionError("Unsupported type of function.");
    }
}
