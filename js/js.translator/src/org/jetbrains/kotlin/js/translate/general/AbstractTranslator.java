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

package org.jetbrains.kotlin.js.translate.general;

import org.jetbrains.kotlin.js.backend.ast.JsProgram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.resolve.BindingContext;

public abstract class AbstractTranslator {

    @NotNull
    private final TranslationContext context;

    protected AbstractTranslator(@NotNull TranslationContext context) {
        this.context = context;
    }

    @NotNull
    protected JsProgram program() {
        return context.program();
    }

    @NotNull
    protected TranslationContext context() {
        return context;
    }

    @NotNull
    protected BindingContext bindingContext() {
        return context.bindingContext();
    }

    @NotNull
    protected Namer namer() {
        return context.namer();
    }
}
