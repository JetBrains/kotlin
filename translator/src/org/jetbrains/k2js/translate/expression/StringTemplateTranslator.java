package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsStringLiteral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

import static com.google.dart.compiler.util.AstUtil.sum;

/**
 * @author Talanov Pavel
 */
public final class StringTemplateTranslator extends AbstractTranslator {

    @NotNull
    public static JsExpression translate(@NotNull JetStringTemplateExpression expression,
                                         @NotNull TranslationContext context) {
        return (new StringTemplateTranslator(expression, context).translate());
    }


    @NotNull
    private final JetStringTemplateExpression expression;

    private StringTemplateTranslator(@NotNull JetStringTemplateExpression expression,
                                     @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
    }

    @NotNull
    private JsExpression translate() {
        assert expression.getEntries().length != 0 : "String template must have one or more entries.";
        EntryVisitor entryVisitor = new EntryVisitor();
        for (JetStringTemplateEntry entry : expression.getEntries()) {
            entry.accept(entryVisitor);
        }
        return entryVisitor.getResultingExpression();
    }

    private final class EntryVisitor extends JetVisitorVoid {

        @Nullable
        private JsExpression resultingExpression = null;

        void append(@NotNull JsExpression expression) {
            if (resultingExpression == null) {
                resultingExpression = expression;
            } else {
                resultingExpression = sum(resultingExpression, expression);
            }
        }

        @Override
        public void visitStringTemplateEntryWithExpression(@NotNull JetStringTemplateEntryWithExpression entry) {
            JetExpression entryExpression = entry.getExpression();
            assert entryExpression != null :
                    "JetStringTemplateEntryWithExpression must have not null entry expression.";
            append(Translation.translateAsExpression(entryExpression, context()));
        }

        //TODO: duplication
        @Override
        public void visitLiteralStringTemplateEntry(@NotNull JetLiteralStringTemplateEntry entry) {
            JsStringLiteral stringConstant = context().program().getStringLiteral(entry.getText());
            append(stringConstant);
        }

        @Override
        public void visitEscapeStringTemplateEntry(@NotNull JetEscapeStringTemplateEntry entry) {
            JsStringLiteral escapedValue = context().program().getStringLiteral(entry.getUnescapedValue());
            append(escapedValue);
        }

        @NotNull
        public JsExpression getResultingExpression() {
            assert resultingExpression != null;
            return resultingExpression;
        }
    }
}
