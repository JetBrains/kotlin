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

import closurecompiler.internal.com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.K2JSTranslator;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.utils.ExceptionUtils.rethrow;
import static org.jetbrains.k2js.test.BasicTest.pathToTestFilesRoot;

/**
 * @author Pavel Talanov
 */
public final class RhinoUtils {

    public static final String KOTLIN_JS_LIB_COMMON = pathToTestFilesRoot() + "kotlin_lib.js";
    private static final String KOTLIN_JS_LIB_ECMA_3 = pathToTestFilesRoot() + "kotlin_lib_ecma3.js";
    private static final String KOTLIN_JS_LIB_ECMA_5 = pathToTestFilesRoot() + "kotlin_lib_ecma5.js";

    private RhinoUtils() {

    }

    private static void runFileWithRhino(@NotNull String inputFile,
            @NotNull Context context,
            @NotNull Scriptable scope) throws Exception {
        FileReader reader = new FileReader(inputFile);
        try {
            context.evaluateReader(scope, reader, inputFile, 1, null);
        }
        finally {
            reader.close();
        }
    }

    public static void runRhinoTest(@NotNull List<String> fileNames,
            @NotNull RhinoResultChecker checker) throws Exception {
        runRhinoTest(fileNames, checker, null, EcmaVersion.defaultVersion());
    }

    public static void runRhinoTest(@NotNull List<String> fileNames,
            @NotNull RhinoResultChecker checker,
            @Nullable Map<String, Object> variables,
            @NotNull EcmaVersion ecmaVersion) throws Exception {
        Context context = createContext(ecmaVersion);
        try {
            Scriptable scope = getScope(ecmaVersion, context);
            putGlobalVariablesIntoScope(scope, variables);
            for (String filename : fileNames) {
                runFileWithRhino(filename, context, scope);
            }
            checker.runChecks(context, scope);
        }
        finally {
            Context.exit();
        }
    }

    @NotNull
    private static Scriptable getScope(@NotNull EcmaVersion version, @NotNull Context context) {
        ScriptableObject scope = context.initStandardObjects(null, false);
        scope.setParentScope(getParentScope(version, context));
        return scope;
    }

    @NotNull
    private static Scriptable getParentScope(@NotNull EcmaVersion version, @NotNull Context context) {
        Scriptable parentScope = versionToScope.get(version);
        if (parentScope == null) {
            parentScope = initScope(version, context);
            versionToScope.put(version, parentScope);
        }
        return parentScope;
    }

    @NotNull
    private static Scriptable initScope(@NotNull EcmaVersion version, @NotNull Context context) {
        ScriptableObject scope = context.initStandardObjects();
        try {
            runFileWithRhino(getKotlinLibFile(version), context, scope);
            runFileWithRhino(KOTLIN_JS_LIB_COMMON, context, scope);
        }
        catch (Exception e) {
            throw rethrow(e);
        }
        scope.sealObject();
        return scope;
    }


    //TODO:
    @NotNull
    private static Context createContext(@NotNull EcmaVersion ecmaVersion) {
        Context context = Context.enter();
        if (ecmaVersion == EcmaVersion.v5) {
            // actually, currently, doesn't matter because dart doesn't produce js 1.8 code (expression closures)
            context.setLanguageVersion(Context.VERSION_1_8);
        }
        return context;
    }

    private static void putGlobalVariablesIntoScope(@NotNull Scriptable scope, @Nullable Map<String, Object> variables) {
        if (variables == null) {
            return;
        }
        Set<Map.Entry<String, Object>> entries = variables.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String name = entry.getKey();
            Object value = entry.getValue();
            scope.put(name, scope, value);
        }
    }

    @NotNull
    private static final Map<EcmaVersion, Scriptable> versionToScope = Maps.newHashMap();

    @NotNull
    public static String getKotlinLibFile(@NotNull EcmaVersion ecmaVersion) {
        return ecmaVersion == EcmaVersion.v5 ? KOTLIN_JS_LIB_ECMA_5 : KOTLIN_JS_LIB_ECMA_3;
    }

    static void flushSystemOut(@NotNull Context context, @NotNull Scriptable scope) {
        context.evaluateString(scope, K2JSTranslator.FLUSH_SYSTEM_OUT, "test", 0, null);
    }
}
