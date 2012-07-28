/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

/**
 * @author Pavel Talanov
 */
public final class TemporaryVariable {

    @NotNull
    private final JsExpression assignmentExpression;
    @NotNull
    private final JsName variableName;
    @NotNull
    private final JsExpression initExpression;

    /*package*/ TemporaryVariable(@NotNull JsName temporaryName, @NotNull JsExpression initExpression) {
        this.variableName = temporaryName;
        this.initExpression = initExpression;
        this.assignmentExpression = JsAstUtils.assignment(variableName.makeRef(), this.initExpression);
    }

    @NotNull
    public JsNameRef reference() {
        return variableName.makeRef();
    }

    @NotNull
    public JsName name() {
        return variableName;
    }

    @NotNull
    public JsExpression assignmentExpression() {
        return assignmentExpression;
    }

    @NotNull
    public JsExpression initExpression() {
        return initExpression;
    }
}