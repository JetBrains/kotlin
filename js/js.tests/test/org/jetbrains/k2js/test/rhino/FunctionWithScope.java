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

package org.jetbrains.k2js.test.rhino;

import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * @author Sergey Simonchik
 */
class FunctionWithScope {
    private final Function fun;
    private final Scriptable scope;

    FunctionWithScope(@NotNull Function function, @NotNull Scriptable scope) {
        this.fun = function;
        this.scope = scope;
    }

    @NotNull
    public Function getFunction() {
        return fun;
    }

    @NotNull
    public Scriptable getScope() {
        return scope;
    }
}
