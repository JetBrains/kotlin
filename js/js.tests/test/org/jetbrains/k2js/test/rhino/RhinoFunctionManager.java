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

    @NotNull
    public String getFunctionName() {
        return functionName;
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
