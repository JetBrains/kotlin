package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.ValueArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class TranslationUtils {

    @NotNull
    static public JsBinaryOperation notNullCheck(@NotNull TranslationContext context,
                                                 @NotNull JsExpression expressionToCheck) {
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        return AstUtil.notEqual(expressionToCheck, nullLiteral);
    }

    @NotNull
    static public JsBinaryOperation isNullCheck(@NotNull TranslationContext context,
                                                @NotNull JsExpression expressionToCheck) {
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        return AstUtil.equals(expressionToCheck, nullLiteral);
    }

    @NotNull
    static public JsNameRef getReference(@NotNull TranslationContext context,
                                         @NotNull JetSimpleNameExpression expression,
                                         @NotNull JsName referencedName) {
        return (new ReferenceProvider(context, expression, referencedName)).generateCorrectReference();
    }


    @Nullable
    static public JsName getLocalReferencedName(@NotNull TranslationContext context,
                                                @NotNull String name) {
        return context.enclosingScope().findExistingName(name);
    }


    @NotNull
    static public List<JsExpression> translateArgumentList(@NotNull List<? extends ValueArgument> jetArguments,
                                                           @NotNull TranslationContext context) {
        List<JsExpression> jsArguments = new ArrayList<JsExpression>();
        for (ValueArgument argument : jetArguments) {
            jsArguments.add(translateArgument(context, argument));
        }
        return jsArguments;
    }

    @NotNull
    static public JsExpression translateArgument(@NotNull TranslationContext context, @NotNull ValueArgument argument) {
        JetExpression jetExpression = argument.getArgumentExpression();
        if (jetExpression == null) {
            throw new AssertionError("Argument with no expression encountered!");
        }
        return Translation.translateAsExpression(jetExpression, context);
    }


}
