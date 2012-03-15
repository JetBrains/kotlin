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

import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class NamespaceInitializerTranslator {

    @NotNull
    private final NamespaceDescriptor namespace;
    @NotNull
    private final TranslationContext namespaceContext;

    public NamespaceInitializerTranslator(@NotNull NamespaceDescriptor namespace, @NotNull TranslationContext context) {
        this.namespace = namespace;
        this.namespaceContext = context;
    }

    @NotNull
    public JsPropertyInitializer generateInitializeMethod() {
        JsFunction result = JsAstUtils.createFunctionWithEmptyBody(namespaceContext.jsScope());
        TranslationContext namespaceInitializerContext
                = namespaceContext.innerContextWithGivenScopeAndBlock(result.getScope(), result.getBody());
        List<JsStatement> initializerStatements =
                (new InitializerVisitor()).traverseNamespace(namespace, namespaceInitializerContext);
        result.getBody().getStatements().addAll(initializerStatements);
        return InitializerUtils.generateInitializeMethod(result);
    }


}
