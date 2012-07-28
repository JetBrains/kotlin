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

package org.jetbrains.k2js.translate.test;

import com.google.dart.compiler.backend.js.ast.JsBlock;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsStringLiteral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;

/**
 * @author Pavel Talanov
 */
public abstract class JSTester {

    @Nullable
    private JsBlock block;

    @Nullable
    private TranslationContext context;

    public JSTester() {
        this.block = null;
        this.context = null;
    }

    public abstract void constructTestMethodInvocation(@NotNull JsExpression call, @NotNull JsStringLiteral name);

    @NotNull
    protected JsBlock getBlock() {
        assert block != null : "Call initialize before using tester.";
        return block;
    }

    @NotNull
    protected TranslationContext getContext() {
        assert context != null : "Call initialize before using tester.";
        return context;
    }

    public void initialize(@NotNull TranslationContext context, @NotNull JsBlock block) {
        this.block = block;
        this.context = context;
    }

    public void deinitialize() {
        this.block = null;
        this.context = null;
    }
}
