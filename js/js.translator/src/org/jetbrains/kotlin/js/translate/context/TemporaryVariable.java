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

package org.jetbrains.kotlin.js.translate.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;

public class TemporaryVariable {

    /*package*/ static TemporaryVariable create(@NotNull JsName temporaryName, @Nullable JsExpression initExpression) {
        JsBinaryOperation rhs = null;
        if (initExpression != null) {
            rhs = JsAstUtils.assignment(temporaryName.makeRef(), initExpression);
            rhs.source(initExpression.getSource());
            MetadataProperties.setSynthetic(rhs, true);
        }
        return new TemporaryVariable(temporaryName, rhs);
    }

    @Nullable
    private final JsExpression assignmentExpression;
    @NotNull
    private final JsName variableName;

    protected TemporaryVariable(@NotNull JsName temporaryName, @Nullable JsExpression assignmentExpression) {
        this.variableName = temporaryName;
        this.assignmentExpression = assignmentExpression;
    }

    @NotNull
    public JsNameRef reference() {
        JsNameRef result = variableName.makeRef();
        MetadataProperties.setSynthetic(result, true);
        return result;
    }

    @NotNull
    public JsName name() {
        return variableName;
    }

    @NotNull
    public JsExpression assignmentExpression() {
        assert assignmentExpression != null;
        return assignmentExpression;
    }

    @NotNull
    public JsStatement assignmentStatement() {
        return JsAstUtils.asSyntheticStatement(assignmentExpression());
    }
}
