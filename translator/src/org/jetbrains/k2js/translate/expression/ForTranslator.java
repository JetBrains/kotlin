package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

/**
 * @author Talanov Pavel
 */
public final class ForTranslator extends AbstractTranslator {

    @NotNull
    public static JsStatement translate(@NotNull JetForExpression expression,
                                        @NotNull TranslationContext context) {
        return (new ForTranslator(expression, context).translate());
    }

    @NotNull
    private final JetForExpression expression;

    private ForTranslator(@NotNull JetForExpression forExpression, @NotNull TranslationContext context) {
        super(context);
        this.expression = forExpression;
    }


    @NotNull
    private JsBlock translate() {
        JetParameter loopParameter = expression.getLoopParameter();
        assert loopParameter != null;
        JsName parameterName = context().declareLocalVariable(loopParameter);
        TemporaryVariable iterator = context().declareTemporary(iteratorMethodInvocation());
        JsBlock bodyBlock = new JsBlock();
        bodyBlock.addStatement(AstUtil.newAssignmentStatement(parameterName.makeRef(), nextMethodInvocation(iterator)));
        bodyBlock.addStatement(AstUtil.convertToBlock(Translation.translateExpression(expression.getBody(), context())));
        JsWhile cycle = new JsWhile(hasNextMethodInvocation(iterator), bodyBlock);
        return AstUtil.newBlock(iterator.assignmentExpression().makeStmt(), cycle);
    }

    @NotNull
    private JsExpression nextMethodInvocation(@NotNull TemporaryVariable iterator) {
        JsNameRef next = AstUtil.newQualifiedNameRef("next");
        JsNameRef result = iterator.nameReference();
        AstUtil.setQualifier(next, result);
        return AstUtil.newInvocation(next);
    }

    @NotNull
    private JsExpression hasNextMethodInvocation(@NotNull TemporaryVariable iterator) {
        JsNameRef hasNext = AstUtil.newQualifiedNameRef("hasNext");
        JsNameRef result = iterator.nameReference();
        AstUtil.setQualifier(hasNext, result);
        return AstUtil.newInvocation(hasNext);
    }


    @NotNull
    private JsExpression iteratorMethodInvocation() {
        JetExpression rangeExpression = expression.getLoopRange();
        assert rangeExpression != null;
        JsExpression range = Translation.translateAsExpression(rangeExpression, context());
        JsNameRef iteratorMethodReference = AstUtil.newQualifiedNameRef("iterator");
        AstUtil.setQualifier(iteratorMethodReference, range);
        return AstUtil.newInvocation(iteratorMethodReference);
    }
}
