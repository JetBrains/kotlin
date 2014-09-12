/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.inline;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import static org.jetbrains.k2js.inline.util.UtilPackage.canHaveSideEffect;

class ReplaceReturnVisitor extends JsVisitorWithContextImpl {
    private final JsNameRef resultRef;

    @Nullable
    private final JsNameRef breakLabel;

    @NotNull
    public static JsNode replaceReturn(JsStatement statement, @Nullable JsNameRef resultRef, @Nullable JsNameRef breakLabel) {
        return new ReplaceReturnVisitor(resultRef, breakLabel).accept(statement);
    }

    private ReplaceReturnVisitor(@Nullable JsNameRef resultRef, @Nullable JsNameRef breakLabel) {
        this.resultRef = resultRef;
        this.breakLabel = breakLabel;
    }

    @Override
    public void endVisit(JsReturn x, JsContext ctx) {
        if (breakLabel != null) {
            JsBreak breakFunction = new JsBreak(breakLabel);
            ctx.insertAfter(breakFunction);
        }

        JsExpression returnExpression = x.getExpression();
        if (returnExpression == null) {
            ctx.removeMe();
            return;
        }

        if (resultRef != null) {
            JsExpression resultAssignment = JsAstUtils.assignment(resultRef, returnExpression);
            ctx.replaceMe(new JsExpressionStatement(resultAssignment));
            return;
        }

        if (canHaveSideEffect(returnExpression)) {
            JsStatement replacement = new JsExpressionStatement(returnExpression);
            ctx.replaceMe(replacement);
        } else {
            ctx.removeMe();
        }
    }

    @Override
    public boolean visit(JsObjectLiteral x, JsContext ctx) {
        return false;
    }

    @Override
    public boolean visit(JsFunction x, JsContext ctx) {
        return false;
    }
}
