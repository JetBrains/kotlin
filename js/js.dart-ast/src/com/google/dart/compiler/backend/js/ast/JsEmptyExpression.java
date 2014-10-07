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

package com.google.dart.compiler.backend.js.ast;

import org.jetbrains.annotations.NotNull;

public class JsEmptyExpression extends JsExpressionImpl {

    JsEmptyExpression() {
    }

    @Override
    @NotNull
    public JsStatement makeStmt() {
        return new JsEmpty();
    }

    @Override
    public void accept(JsVisitor visitor) {
        throw new IllegalArgumentException("empty expression should not be here during generating Javascript code");
    }

    @Override
    public void traverse(JsVisitorWithContext visitor, JsContext ctx) {
        throw new IllegalArgumentException("empty expression should not be here during generating Javascript code");
    }

    @NotNull
    @Override
    public JsEmptyExpression deepCopy() {
        return new JsEmptyExpression();
    }
}
