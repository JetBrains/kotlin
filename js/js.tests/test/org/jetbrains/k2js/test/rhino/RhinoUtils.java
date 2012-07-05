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
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.K2JSTranslator;
import org.jetbrains.k2js.test.BasicTest;
import org.mozilla.javascript.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.utils.ExceptionUtils.rethrow;
import static org.jetbrains.k2js.test.BasicTest.pathToTestFilesRoot;

/**
 * @author Pavel Talanov
 */
public final class RhinoUtils {
    @NotNull
    private static final Set<String> IGNORED_JSLINT_WARNINGS = Sets.newHashSet();

    static {
        // todo dart ast bug
        IGNORED_JSLINT_WARNINGS.add("Unexpected space between '}' and '('.");
        // don't read JS, use kotlin and idea debugger ;)
        IGNORED_JSLINT_WARNINGS
                .add("Wrap an immediate function invocation in parentheses to assist the reader in understanding that the expression is the result of a function, and not the function itself.");
    }

    @NotNull
    private static final RhinoFunctionManager functionManager = new RhinoFunctionManager(
            new Supplier<String>() {
                @Override
                public String get() {
                    return fileToString(BasicTest.JSLINT_LIB);
                }
            },
            "JSLINT"
    );


    public static final String KOTLIN_JS_LIB_COMMON = pathToTestFilesRoot() + "kotlin_lib.js";
    private static final String KOTLIN_JS_LIB_ECMA_3 = pathToTestFilesRoot() + "kotlin_lib_ecma3.js";
    private static final String KOTLIN_JS_LIB_ECMA_5 = pathToTestFilesRoot() + "kotlin_lib_ecma5.js";

    private RhinoUtils() {

    }

    private static String fileToString(String file) {
        try {
            return FileUtil.loadFile(new File(file));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
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
       runRhinoTest(fileNames, checker, variables, ecmaVersion, Collections.EMPTY_LIST);
    }

    public static void runRhinoTest(@NotNull List<String> fileNames,
            @NotNull RhinoResultChecker checker,
            @Nullable Map<String, Object> variables,
            @NotNull EcmaVersion ecmaVersion,
            @NotNull List<String> jsLibraries) throws Exception {
        Context context = createContext(ecmaVersion);
        try {
            Scriptable scope = getScope(ecmaVersion, context, jsLibraries);
            putGlobalVariablesIntoScope(scope, variables);
            for (String filename : fileNames) {
                runFileWithRhino(filename, context, scope);
            }
            checker.runChecks(context, scope);

            lintIt(context, fileNames.get(fileNames.size() - 1));
        }
        finally {
            Context.exit();
        }
    }

    @NotNull
    private static Scriptable getScope(@NotNull EcmaVersion version, @NotNull Context context,
                @NotNull List<String> jsLibraries) {
        ScriptableObject scope = context.initStandardObjects(null, false);
        scope.setParentScope(getParentScope(version, context, jsLibraries));
        return scope;
    }

    @NotNull
    private static Scriptable getParentScope(@NotNull EcmaVersion version, @NotNull Context context,
                @NotNull List<String> jsLibraries) {
        Scriptable parentScope = versionToScope.get(version);
        if (parentScope == null) {
            parentScope = initScope(version, context, jsLibraries);
            versionToScope.put(version, parentScope);
        }
        return parentScope;
    }

    @NotNull
    private static Scriptable initScope(@NotNull EcmaVersion version, @NotNull Context context,
                @NotNull List<String> jsLibraries) {
        ScriptableObject scope = context.initStandardObjects(null, false);
        try {
            runFileWithRhino(getKotlinLibFile(version), context, scope);
            runFileWithRhino(KOTLIN_JS_LIB_COMMON, context, scope);
            for (String jsLibrary : jsLibraries) {
                runFileWithRhino(jsLibrary, context, scope);
            }
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

    private static void lintIt(Context context, String fileName) throws IOException {
        if (Boolean.valueOf(System.getProperty("test.lint.skip"))) {
            return;
        }

        NativeObject options = new NativeObject();
        // todo fix dart ast?
        options.defineProperty("white", true, ScriptableObject.READONLY);
        // vars, http://uxebu.com/blog/2010/04/02/one-var-statement-for-one-variable/
        options.defineProperty("vars", true, ScriptableObject.READONLY);
        NativeArray globals = new NativeArray(new Object[] {"Kotlin"});
        options.defineProperty("predef", globals, ScriptableObject.READONLY);

        Object[] args = {FileUtil.loadFile(new File(fileName)), options};
        FunctionWithScope functionWithScope = functionManager.getFunctionWithScope();
        Function function = functionWithScope.getFunction();
        Scriptable scope = functionWithScope.getScope();
        Object status = function.call(context, scope, scope, args);
        Boolean noErrors = (Boolean) Context.jsToJava(status, Boolean.class);
        if (!noErrors) {
            Object errors = function.get("errors", scope);
            if (errors == null) {
                return;
            }

            System.out.println(fileName);
            for (Object errorObj : ((NativeArray) errors)) {
                if (!(errorObj instanceof NativeObject)) {
                    continue;
                }

                NativeObject e = (NativeObject) errorObj;
                int line = toInt(e.get("line"));
                int character = toInt(e.get("character"));
                if (line < 0 || character < 0) {
                    continue;
                }
                Object reasonObj = e.get("reason");
                if (reasonObj instanceof String) {
                    String reason = (String) reasonObj;
                    if (IGNORED_JSLINT_WARNINGS.contains(reason)) {
                        continue;
                    }

                    System.out.println(line + ":" + character + " " + reason);
                }
            }
        }
    }

    private static int toInt(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return -1;
    }
}
