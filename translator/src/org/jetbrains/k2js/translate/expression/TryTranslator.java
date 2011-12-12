package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.JsBlock;
import com.google.dart.compiler.backend.js.ast.JsCatch;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsTry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.ArrayList;
import java.util.List;

import static com.google.dart.compiler.util.AstUtil.convertToBlock;

/**
 * @author Talanov Pavel
 */
public final class TryTranslator extends AbstractTranslator {

    @NotNull
    public static JsTry translate(@NotNull JetTryExpression expression,
                                  @NotNull TranslationContext context) {
        return (new TryTranslator(expression, context)).translate();
    }

    @NotNull
    private final JetTryExpression expression;

    private TryTranslator(@NotNull JetTryExpression expression,
                          @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
    }

    private JsTry translate() {
        JsTry result = new JsTry();
        result.setTryBlock(translateTryBlock());
        result.setFinallyBlock(translateFinallyBlock());
        result.getCatches().addAll(translateCatches());
        return result;
    }

    @Nullable
    private JsBlock translateFinallyBlock() {
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        if (finallyBlock == null) return null;

        return convertToBlock(Translation.translateAsStatement(finallyBlock.getFinalExpression(), context()));
    }

    @NotNull
    private JsBlock translateTryBlock() {
        return convertToBlock(Translation.translateAsStatement(expression.getTryBlock(), context()));
    }


    @NotNull
    private List<JsCatch> translateCatches() {
        List<JsCatch> result = new ArrayList<JsCatch>();
        for (JetCatchClause catchClause : expression.getCatchClauses()) {
            result.add(translateCatchClause(catchClause));
        }
        return result;
    }

    @NotNull
    private JsCatch translateCatchClause(@NotNull JetCatchClause catchClause) {
        JetParameter catchParameter = catchClause.getCatchParameter();
        assert catchParameter != null : "Valid catch must have a parameter.";

        JsName parameterName = context().declareLocalVariable(catchParameter);
        JsCatch result = new JsCatch(context().jsScope(), parameterName.getIdent());
        result.setBody(translateCatchBody(catchClause));
        return result;
    }

    @NotNull
    private JsBlock translateCatchBody(@NotNull JetCatchClause catchClause) {
        JetExpression catchBody = catchClause.getCatchBody();
        if (catchBody == null) {
            return convertToBlock(program().getEmptyStmt());
        }
        return convertToBlock(Translation.translateAsStatement(catchBody, context()));
    }

}
