package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsConditional;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNullLiteral;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSafeQualifiedExpression;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static com.google.dart.compiler.util.AstUtil.newSequence;

/**
 * @author Pavel Talanov
 */
public enum CallType {
    SAFE {
        @NotNull
        @Override
        JsExpression mutateSelector(@NotNull JsExpression selector, @NotNull TranslationContext context) {
            TemporaryVariable temporaryVariable = context.declareTemporary(selector);
            return newSequence(temporaryVariable.assignmentExpression(), temporaryVariable.nameReference());
        }

        @NotNull
        @Override
        JsExpression mutateExpression(@NotNull JsExpression mutatedSelector,
                                      @NotNull JsExpression expressionWithMutatedSelector,
                                      @NotNull TranslationContext context) {
            JsNullLiteral nullLiteral = context.program().getNullLiteral();
            //TODO: find similar not null checks
            JsBinaryOperation notNullCheck = AstUtil.notEqual(expressionWithMutatedSelector, nullLiteral);
            return new JsConditional(notNullCheck, expressionWithMutatedSelector, nullLiteral);
        }
    },
    //TODO: bang qualifier is not implemented in frontend for now
    // BANG,
    NORMAL {
        @NotNull
        @Override
        JsExpression mutateSelector(@NotNull JsExpression selector, @NotNull TranslationContext context) {
            // do not mutate
            return selector;
        }

        @NotNull
        @Override
        JsExpression mutateExpression(@NotNull JsExpression mutatedSelector,
                                      @NotNull JsExpression expression,
                                      @NotNull TranslationContext context) {
            // do not mutate
            return expression;
        }
    };

    @NotNull
    abstract JsExpression mutateSelector(@NotNull JsExpression selector, @NotNull TranslationContext context);

    @NotNull
    abstract JsExpression mutateExpression(@NotNull JsExpression mutatedSelector,
                                           @NotNull JsExpression expression,
                                           @NotNull TranslationContext context);

    @NotNull
    public static CallType getCallTypeForQualifierExpression(@NotNull JetQualifiedExpression expression) {
        if (expression instanceof JetSafeQualifiedExpression) {
            return SAFE;
        }
        assert expression instanceof JetDotQualifiedExpression;
        return NORMAL;
    }

}