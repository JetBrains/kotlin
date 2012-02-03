package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;

/**
 * @author Pavel Talanov
 */
public final class NamingScope {

    @NotNull
    public static NamingScope rootScope(@NotNull JsScope rootScope) {
        return new NamingScope(rootScope);
    }

    @NotNull
    private final JsScope scope;

    private NamingScope(@NotNull JsScope correspondingScope) {
        this.scope = correspondingScope;
    }

    @NotNull
    public NamingScope innerScope(@NotNull String scopeName) {
        JsScope innerJsScope = new JsScope(jsScope(), scopeName);
        return new NamingScope(innerJsScope);
    }

    @NotNull
    public NamingScope innerScope(@NotNull JsScope correspondingScope) {
        return new NamingScope(correspondingScope);
    }

    @NotNull
        /*package*/ JsName declareUnobfuscatableName(@NotNull String name) {
        JsName declaredName = scope.declareName(name);
        declaredName.setObfuscatable(false);
        return declaredName;
    }

    //TODO: temporary solution
    @NotNull
    private String mayBeObfuscateName(@NotNull String name, boolean shouldObfuscate) {
        if (!shouldObfuscate) {
            return name;
        }
        return doObfuscate(name);
    }

    @NotNull
    private String doObfuscate(@NotNull String name) {
        int obfuscate = 0;
        String result = name;
        while (true) {
            JsName existingNameWithSameIdent = scope.findExistingName(result);
            boolean isDuplicate = (existingNameWithSameIdent != null) && (scope.ownsName(existingNameWithSameIdent));

            if (!isDuplicate) break;

            result = name + "$" + obfuscate;
            obfuscate++;
        }
        return result;
    }

    @NotNull
        /*package*/ JsName declareObfuscatableName(@NotNull String name) {
        return scope.declareName(mayBeObfuscateName(name, true));
    }

    public JsName declareTemporary() {
        return scope.declareTemporary();
    }

    @NotNull
    public JsScope jsScope() {
        return scope;
    }
}
