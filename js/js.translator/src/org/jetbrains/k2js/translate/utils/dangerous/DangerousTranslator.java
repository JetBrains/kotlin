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

package org.jetbrains.k2js.translate.utils.dangerous;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNode;
import com.google.dart.compiler.backend.js.ast.JsVars;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DangerousTranslator extends AbstractTranslator {
    @NotNull
    private final DangerousData data;

    @NotNull
    public static JsNode translate(@NotNull DangerousData data, @NotNull TranslationContext context) {
        assert data.exists();
        return new DangerousTranslator(data, context).translate();
    }

    private DangerousTranslator(@NotNull DangerousData data, @NotNull TranslationContext context) {
        super(context);
        this.data = data;
    }

    @NotNull
    private JsNode translate() {
        Map<JetExpression, JsName> aliasesForExpressions =
            translateAllExpressionsAndCreateAliasesForThem(data.getNodesToBeGeneratedBefore());
        TranslationContext contextWithAliases = context().innerContextWithAliasesForExpressions(aliasesForExpressions);
        return Translation.doTranslateExpression(data.getRootNode(), contextWithAliases);
    }

    @NotNull
    private Map<JetExpression, JsName> translateAllExpressionsAndCreateAliasesForThem(@NotNull List<JetExpression> expressions) {
        Map<JetExpression, JsName> aliasesForExpressions = new THashMap<JetExpression, JsName>(expressions.size());
        List<JsVars.JsVar> vars = new ArrayList<JsVars.JsVar>(expressions.size());
        for (JetExpression expression : expressions) {
            JsExpression translatedExpression = Translation.translateAsExpression(expression, context());
            JsVars.JsVar aliasForExpression = context().dynamicContext().createTemporaryVar(translatedExpression);
            vars.add(aliasForExpression);
            aliasesForExpressions.put(expression, aliasForExpression.getName());
        }
        context().addStatementToCurrentBlock(new JsVars(vars, true));
        return aliasesForExpressions;
    }
}
