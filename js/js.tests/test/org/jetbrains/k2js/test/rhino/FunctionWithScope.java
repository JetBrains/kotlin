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
