/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.NamingScope;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public abstract class AbstractInitializerTranslator extends AbstractTranslator {

    @NotNull
    private final InitializerVisitor visitor;
    @NotNull
    protected final NamingScope initializerMethodScope;

    protected AbstractInitializerTranslator(@NotNull NamingScope scope, @NotNull TranslationContext context) {
        super(context.contextWithScope(scope));
        this.visitor = new InitializerVisitor();
        this.initializerMethodScope = scope;
    }

    abstract protected JsFunction generateInitializerFunction();

    @NotNull
    public JsPropertyInitializer generateInitializeMethod() {
        JsPropertyInitializer initializer = new JsPropertyInitializer();
        initializer.setLabelExpr(Namer.initializeMethodReference());
        initializer.setValueExpr(generateInitializerFunction());
        return initializer;
    }

    @NotNull
    protected List<JsStatement> translateClassInitializers(@NotNull JetClassOrObject declaration) {
        return visitor.traverseClass(declaration, context());
    }

    @NotNull
    protected List<JsStatement> translateNamespaceInitializers(@NotNull NamespaceDescriptor namespace) {
        return visitor.traverseNamespace(namespace, context());

    }
}
