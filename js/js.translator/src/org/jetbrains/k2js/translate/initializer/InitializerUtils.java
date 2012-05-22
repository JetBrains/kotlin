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
import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import static org.jetbrains.k2js.translate.utils.TranslationUtils.assignmentToBackingField;

/**
 * @author Pavel Talanov
 */
public final class InitializerUtils {

    private InitializerUtils() {
    }

    @NotNull
    public static JsPropertyInitializer generateInitializeMethod(@NotNull JsFunction initializerFunction) {
        JsPropertyInitializer initializer = new JsPropertyInitializer();
        initializer.setLabelExpr(Namer.initializeMethodReference());
        initializer.setValueExpr(initializerFunction);
        return initializer;
    }

    @NotNull
    public static JsStatement generateInitializerForProperty(@NotNull TranslationContext context,
            @NotNull PropertyDescriptor descriptor,
            @NotNull JsExpression value) {
        if (context.isEcma5()) {
            return JsAstUtils.definePropertyDataDescriptor(descriptor, value, context).makeStmt();
        }
        else {
            return assignmentToBackingField(context, descriptor, value).makeStmt();
        }
    }
}
