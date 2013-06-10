/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsNumberLiteral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.sum;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getNameIfStandardType;


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
            }
            else {
                resultingExpression = sum(resultingExpression, expression);
            }
        }

        @Override
        public void visitStringTemplateEntryWithExpression(@NotNull JetStringTemplateEntryWithExpression entry) {
            JetExpression entryExpression = entry.getExpression();
            assert entryExpression != null :
                    "JetStringTemplateEntryWithExpression must have not null entry expression.";
            JsExpression translatedExpression = Translation.translateAsExpression(entryExpression, context());
            if (translatedExpression instanceof JsNumberLiteral) {
                append(context().program().getStringLiteral(translatedExpression.toString()));
                return;
            }
            if (mustCallToString(entryExpression)) {
                append(new JsInvocation(new JsNameRef("toString", translatedExpression)));
            } else {
                append(translatedExpression);
            }
        }

        private boolean mustCallToString(@NotNull JetExpression entryExpression) {
            Name typeName = getNameIfStandardType(entryExpression, context());
            if (typeName == null) {
                return true;
            }
            //TODO: this is a hacky optimization, should use some generic approach
            if (typeName.asString().equals("String")) {
                return false;
            }
            if (typeName.asString().equals("Int") && resultingExpression != null) {
                return false;
            }
            return true;
        }

        @Override
        public void visitLiteralStringTemplateEntry(@NotNull JetLiteralStringTemplateEntry entry) {
            appendText(entry.getText());
        }

        @Override
        public void visitEscapeStringTemplateEntry(@NotNull JetEscapeStringTemplateEntry entry) {
            appendText(entry.getUnescapedValue());
        }

        private void appendText(@NotNull String text) {
            append(program().getStringLiteral(text));
        }

        @NotNull
        public JsExpression getResultingExpression() {
            assert resultingExpression != null;
            return resultingExpression;
        }
    }
}
