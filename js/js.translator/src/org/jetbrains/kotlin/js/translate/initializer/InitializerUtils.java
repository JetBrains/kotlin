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

package org.jetbrains.kotlin.js.translate.initializer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsStatement;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.resolve.source.PsiSourceElementKt;

import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.assignmentToBackingField;

public final class InitializerUtils {
    private InitializerUtils() {
    }

    @NotNull
    public static JsStatement generateInitializerForProperty(
            @NotNull TranslationContext context,
            @NotNull PropertyDescriptor descriptor,
            @NotNull JsExpression value
    ) {
        JsExpression assignment = assignmentToBackingField(context, descriptor, value);
        assignment.setSource(PsiSourceElementKt.getPsi(descriptor.getSource()));
        return assignment.makeStmt();
    }

    @NotNull
    public static JsStatement generateInitializerForDelegate(
            @NotNull TranslationContext context,
            @NotNull PropertyDescriptor descriptor,
            @NotNull JsExpression value
    ) {
        return JsAstUtils.defineSimpleProperty(context.getNameForBackingField(descriptor), value, descriptor.getSource());
    }
}
