/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.expression;

import org.jetbrains.kotlin.js.backend.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsKt;
import org.jetbrains.kotlin.js.patterns.NamePredicate;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.TopLevelFIF;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.js.translate.utils.ErrorReportingUtils.message;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.sum;

public final class StringTemplateTranslator extends AbstractTranslator {
    private final KtStringTemplateEntry[] expressionEntries;

    @NotNull
    public static JsExpression translate(@NotNull KtStringTemplateExpression expression,
                                         @NotNull TranslationContext context) {
        return (new StringTemplateTranslator(expression, context).translate());
    }

    private StringTemplateTranslator(@NotNull KtStringTemplateExpression expression,
                                     @NotNull TranslationContext context) {
        super(context);

        expressionEntries = expression.getEntries();
        assert expressionEntries.length != 0 : message(expression, "String template must have one or more entries.");
    }

    @NotNull
    private JsExpression translate() {
        EntryVisitor entryVisitor = new EntryVisitor();
        for (KtStringTemplateEntry entry : expressionEntries) {
            entry.accept(entryVisitor);
        }
        return entryVisitor.getResultingExpression();
    }

    private final class EntryVisitor extends KtVisitorVoid {

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
        public void visitStringTemplateEntryWithExpression(@NotNull KtStringTemplateEntryWithExpression entry) {
            KtExpression entryExpression = entry.getExpression();
            assert entryExpression != null :
                    "JetStringTemplateEntryWithExpression must have not null entry expression.";
            JsExpression translatedExpression = Translation.translateAsExpression(entryExpression, context());

            KotlinType type = context().bindingContext().getType(entryExpression);

            if (type != null && KotlinBuiltIns.isCharOrNullableChar(type)) {
                if (type.isMarkedNullable()) {
                    TemporaryVariable tmp = context().declareTemporary(translatedExpression, entry);
                    append(new JsConditional(JsAstUtils.equality(tmp.assignmentExpression(), new JsNullLiteral()),
                                             new JsNullLiteral(),
                                             JsAstUtils.charToString(tmp.reference())));
                }
                else {
                    append(JsAstUtils.charToString(translatedExpression));
                }
            }
            else if (translatedExpression instanceof JsNumberLiteral) {
                append(new JsStringLiteral(translatedExpression.toString()));
            }
            else if (type == null || type.isMarkedNullable()) {
                append(TopLevelFIF.TO_STRING.apply((JsExpression) null, new SmartList<>(translatedExpression), context()));
            }
            else if (mustCallToString(type)) {
                append(new JsInvocation(new JsNameRef("toString", translatedExpression)));
            }
            else {
                append(translatedExpression);
            }
        }

        private boolean mustCallToString(@NotNull KotlinType type) {
            Name typeName = DescriptorUtilsKt.getNameIfStandardType(type);
            //TODO: this is a hacky optimization, should use some generic approach

            // Long has valueOf method which will be called instead of toString and produce different result.
            if (KotlinBuiltIns.isAny(type) ||
                KotlinBuiltIns.isComparable(type) ||
                KotlinBuiltIns.isNumber(type) ||
                KotlinBuiltIns.isLong(type)
            ) {
                return true;
            }

            if (typeName != null && NamePredicate.STRING.test(typeName)) {
                return false;
            }
            return resultingExpression == null;
        }

        @Override
        public void visitLiteralStringTemplateEntry(@NotNull KtLiteralStringTemplateEntry entry) {
            appendText(entry.getText());
        }

        @Override
        public void visitEscapeStringTemplateEntry(@NotNull KtEscapeStringTemplateEntry entry) {
            appendText(entry.getUnescapedValue());
        }

        private void appendText(@NotNull String text) {
            append(new JsStringLiteral(text));
        }

        @NotNull
        public JsExpression getResultingExpression() {
            assert resultingExpression != null;
            return resultingExpression;
        }
    }
}
