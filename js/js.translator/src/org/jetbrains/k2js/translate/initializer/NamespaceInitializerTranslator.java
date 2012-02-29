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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.newBlock;

/**
 * @author Pavel Talanov
 */
public final class NamespaceInitializerTranslator extends AbstractInitializerTranslator {

    @NotNull
    private final NamespaceDescriptor namespace;

    public NamespaceInitializerTranslator(@NotNull NamespaceDescriptor namespace, @NotNull TranslationContext context) {
        //NOTE:
        super(context.getScopeForDescriptor(namespace), context);
        this.namespace = namespace;
    }

    @Override
    @NotNull
    protected JsFunction generateInitializerFunction() {
        //NOTE: namespace has no constructor
        JsFunction result = new JsFunction(initializerMethodScope.jsScope());
        result.setBody(newBlock(translateNamespaceInitializers(namespace)));
        return result;
    }


}
