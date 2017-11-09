/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.utils.mutator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.backend.ast.*;

import java.util.List;

import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.convertToStatement;

public final class LastExpressionMutator {
    public static JsStatement mutateLastExpression(@NotNull JsNode node, @NotNull Mutator mutator) {
        return convertToStatement(new LastExpressionMutator(mutator).apply(node));
    }

    @NotNull
    private final Mutator mutator;

    private LastExpressionMutator(@NotNull Mutator mutator) {
        this.mutator = mutator;
    }

    //TODO: visitor?
    //TODO: when expression?
    @NotNull
    private JsNode apply(@NotNull JsNode node) {
        if (node instanceof JsBlock) {
            return applyToBlock((JsBlock) node);
        }
        if (node instanceof JsIf) {
            return applyToIf((JsIf) node);
        }
        if (node instanceof JsTry) {
            return applyToTry((JsTry) node);
        }
        if (node instanceof JsExpressionStatement) {
            return applyToStatement((JsExpressionStatement) node);
        }
        return mutator.mutate(node);
    }

    @NotNull
    private JsNode applyToStatement(@NotNull JsExpressionStatement node) {
        return convertToStatement(apply(node.getExpression()));
    }

    @NotNull
    private JsNode applyToIf(@NotNull JsIf node) {
        node.setThenStatement(convertToStatement(apply(node.getThenStatement())));
        JsStatement elseStmt = node.getElseStatement();
        if (elseStmt != null) {
            node.setElseStatement(convertToStatement(apply(elseStmt)));
        }
        return node;
    }

    @NotNull
    private JsNode applyToTry(@NotNull JsTry node) {
        applyToBlock(node.getTryBlock());
        for(JsCatch jsCatch: node.getCatches()) {
            applyToBlock(jsCatch.getBody());
        }
        return node;
    }

    @NotNull
    private JsNode applyToBlock(@NotNull JsBlock node) {
        List<JsStatement> statements = node.getStatements();

        if (statements.isEmpty()) return node;

        int size = statements.size();
        statements.set(size - 1, convertToStatement(apply(statements.get(size - 1))));
        return node;
    }
}
