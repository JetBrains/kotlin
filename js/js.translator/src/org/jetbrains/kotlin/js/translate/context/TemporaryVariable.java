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
import org.jetbrains.kotlin.types.KotlinType;

public class TemporaryVariable {

    /*package*/ static TemporaryVariable create(@NotNull JsName temporaryName, @Nullable JsExpression initExpression) {
        JsBinaryOperation rhs = null;
        KotlinType type = null;
        if (initExpression != null) {
            rhs = JsAstUtils.assignment(temporaryName.makeRef(), initExpression);
            rhs.source(initExpression.getSource());
            MetadataProperties.setSynthetic(rhs, true);
            type = MetadataProperties.getType(initExpression);
        }
        return new TemporaryVariable(temporaryName, rhs, type);
    }

    @Nullable
    private final JsExpression assignmentExpression;
    @NotNull
    private final JsName variableName;
    @Nullable
    private final KotlinType type;

    protected TemporaryVariable(@NotNull JsName temporaryName, @Nullable JsExpression assignmentExpression, @Nullable KotlinType type) {
        this.variableName = temporaryName;
        this.assignmentExpression = assignmentExpression;
        this.type = type;
    }

    @NotNull
    public JsNameRef reference() {
        JsNameRef result = variableName.makeRef();
        MetadataProperties.setSynthetic(result, true);
        MetadataProperties.setType(result, type);
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
