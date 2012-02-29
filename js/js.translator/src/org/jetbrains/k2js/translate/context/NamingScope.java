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

package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Basically a wrapper around JsScope.
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
        return innerScope(innerJsScope);
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

    @NotNull
        /*package*/ JsName declareObfuscatableName(@NotNull String name) {
        return scope.declareName(mayBeObfuscateName(name, true));
    }

    //TODO: temporary solution
    @NotNull
    private String mayBeObfuscateName(@NotNull String name, boolean shouldObfuscate) {
        if (!shouldObfuscate) {
            return name;
        }
        return doObfuscate(name);
    }

    //TODO: refactor
    @NotNull
    private String doObfuscate(@NotNull String name) {
        int obfuscate = 0;
        String result = name;
        while (true) {
            JsName existingNameWithSameIdent = scope.findExistingName(result);
            boolean isDuplicate = (existingNameWithSameIdent != null) && JsAstUtils.ownsName(scope, existingNameWithSameIdent);

            if (!isDuplicate) break;

            result = name + "$" + obfuscate;
            obfuscate++;
        }
        return result;
    }


    @NotNull
    public JsName declareTemporary() {
        return scope.declareTemporary();
    }

    @NotNull
    public JsScope jsScope() {
        return scope;
    }
}
