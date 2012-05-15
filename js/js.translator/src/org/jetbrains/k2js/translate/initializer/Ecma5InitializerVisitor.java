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

package org.jetbrains.k2js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsObjectLiteral;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.List;

public final class Ecma5InitializerVisitor extends InitializerVisitor {
    @NotNull
    private final JsObjectLiteral propertiesDefinition = new JsObjectLiteral();

    @Override
    @Nullable
    protected JsExpression generateInitializerForProperty(@NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JsExpression value, @NotNull TranslationContext context) {
        propertiesDefinition.getPropertyInitializers().add(JsAstUtils.propertyDescriptor(propertyDescriptor, value, context));
        return null;
    }

    @NotNull
    @Override
    protected List<JsStatement> generateInitializerStatements(@NotNull List<JetDeclaration> declarations,
            @NotNull TranslationContext context) {
        List<JsStatement> statements = super.generateInitializerStatements(declarations, context);
        if (!propertiesDefinition.getPropertyInitializers().isEmpty()) {
            JsStatement statement = JsAstUtils.defineProperties(propertiesDefinition);
            statements.add(0, statement);
        }
        return statements;
    }
}
