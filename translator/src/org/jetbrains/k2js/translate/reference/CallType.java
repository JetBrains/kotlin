package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsConditional;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNullLiteral;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
        JsExpression constructCall(@Nullable JsExpression receiver, @NotNull CallConstructor constructor,
                                   @NotNull TranslationContext context) {
            assert receiver != null;
            TemporaryVariable temporaryVariable = context.declareTemporary(receiver);
            JsNullLiteral nullLiteral = context.program().getNullLiteral();
            //TODO: find similar not null checks
            JsBinaryOperation notNullCheck = AstUtil.notEqual(temporaryVariable.nameReference(), nullLiteral);
            JsConditional callMethodIfNotNullElseNull =
                    new JsConditional(notNullCheck, constructor.construct(temporaryVariable.nameReference()), nullLiteral);
            return newSequence(temporaryVariable.assignmentExpression(), callMethodIfNotNullElseNull);
        }
    },
    //TODO: bang qualifier is not implemented in frontend for now
    // BANG,
    NORMAL {
        @NotNull
        @Override
        JsExpression constructCall(@Nullable JsExpression receiver, @NotNull CallConstructor constructor,
                                   @NotNull TranslationContext context) {
            return constructor.construct(receiver);
        }
    };

    @NotNull
    abstract JsExpression constructCall(@Nullable JsExpression receiver, @NotNull CallConstructor constructor,
                                        @NotNull TranslationContext context);

    @NotNull
    public static CallType getCallTypeForQualifiedExpression(@NotNull JetQualifiedExpression expression) {
        if (expression instanceof JetSafeQualifiedExpression) {
            return SAFE;
        }
        assert expression instanceof JetDotQualifiedExpression;
        return NORMAL;
    }

    public interface CallConstructor {
        @NotNull
        JsExpression construct(@Nullable JsExpression receiver);
    }

}