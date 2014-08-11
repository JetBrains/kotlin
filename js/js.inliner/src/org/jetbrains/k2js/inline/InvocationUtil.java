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

import java.util.List;

class InvocationUtil {

    /**
     * Tests if result of call is being used in containing statement
     */
    public static boolean isResultUsed(JsStatement containingStatement, JsInvocation call) {
        return !(containingStatement instanceof JsExpressionStatement)
               || call != ((JsExpressionStatement) containingStatement).getExpression();
    }

    @Nullable
    public static JsName getName(@NotNull JsInvocation call) {
        JsExpression qualifier = call.getQualifier();
        if (qualifier instanceof JsNameRef) {
            return ((JsNameRef) qualifier).getName();
        }

        return null;
    }

    /**
     * Gets inner function from function, that creates closure
     *
     * For example:
     * function(a) {
     *   return function() { return a; }
     * }
     *
     * Inner functions can only be generated when lambda
     * with closure is created
     */
    @Nullable
    public static JsFunction getInnerFunction(@NotNull JsFunction outer) {
        List<JsStatement> statements = outer.getBody().getStatements();
        if (statements.size() != 1) {
            return null;
        }

        JsStatement statement = statements.get(0);
        if (!(statement instanceof JsReturn)) {
            return null;
        }

        JsExpression returnExpr = ((JsReturn) statement).getExpression();
        if (!(returnExpr instanceof JsFunction)) {
            return null;
        }

        return (JsFunction) returnExpr;
    }
}
