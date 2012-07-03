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

import com.google.common.base.Supplier;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/**
 * @author Sergey Simonchik
 */
class RhinoFunctionManager {
    private static final Logger LOG = Logger.getInstance(RhinoFunctionManager.class);

    private final ThreadLocal<FunctionWithScope> threadLocalFunction = new ThreadLocal<FunctionWithScope>() {
        @Override
        protected FunctionWithScope initialValue() {
            if (script == null) {
                synchronized (threadLocalFunction) {
                    if (script == null) {
                        script = compileScript(9);
                    }
                }
            }
            return extractFunctionWithScope(script);
        }
    };

    private volatile Script script;

    private final Supplier<String> scriptSourceProvider;
    private final String functionName;

    public RhinoFunctionManager(@NotNull Supplier<String> scriptSourceProvider,
            @NotNull String functionName) {
        this.scriptSourceProvider = scriptSourceProvider;
        this.functionName = functionName;
    }

    private Script compileScript(int optimizationLevel) {
        long startNano = System.nanoTime();
        Context context = Context.enter();
        try {
            context.setOptimizationLevel(optimizationLevel);
            String scriptSource = scriptSourceProvider.get();
            return context.compileString(scriptSource, "<" + functionName + " script>", 1, null);
        }
        finally {
            Context.exit();
            LOG.info(formatMessage(startNano, functionName + " script rhino compilation"));
        }
    }

    @NotNull
    private FunctionWithScope extractFunctionWithScope(@NotNull Script script) {
        long startNano = System.nanoTime();
        Context context = Context.enter();
        try {
            Scriptable scope = context.initStandardObjects();
            script.exec(context, scope);
            Object jsLintObj = scope.get(functionName, scope);
            if (jsLintObj instanceof Function) {
                Function jsLint = (Function) jsLintObj;
                return new FunctionWithScope(jsLint, scope);
            }
            else {
                throw new RuntimeException(functionName + " is undefined or not a function.");
            }
        }
        finally {
            Context.exit();
            LOG.info(formatMessage(startNano, functionName + " function extraction"));
        }
    }

    private static String formatMessage(long startTimeNano, @NotNull String actionName) {
        long nanoDuration = System.nanoTime() - startTimeNano;
        return String.format("[%s] %s took %.2f ms",
                             Thread.currentThread().getName(),
                             actionName,
                             nanoDuration / 1000000.0);
    }

    @NotNull
    public FunctionWithScope getFunctionWithScope() {
        return threadLocalFunction.get();
    }
}
