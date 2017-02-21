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

package org.jetbrains.kotlin.js.test.rhino;

import com.google.common.collect.Sets;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.mozilla.javascript.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.js.test.BasicTest.DIST_DIR_JS_PATH;
import static org.jetbrains.kotlin.js.test.BasicTest.TEST_DATA_DIR_PATH;

public final class RhinoUtils {
    private static final String KOTLIN_JS_LIB_ECMA_5 = TEST_DATA_DIR_PATH + "kotlin_lib_ecma5.js";

    private static final Set<String> IGNORED_JSHINT_WARNINGS = Sets.newHashSet();
    
    private static final NativeObject JSHINT_OPTIONS = new NativeObject();

    public static final String OPTIMIZATION_LEVEL_TEST_VARIABLE = "rhinoOptimizationLevel";

    public static final int OPTIMIZATION_OFF = -1;

    private static final int OPTIMIZATION_DEFAULT = 0;

    private static final String SETUP_KOTLIN_OUTPUT = "kotlin.kotlin.io.output = new kotlin.kotlin.io.BufferedOutput();";
    private static final String FLUSH_KOTLIN_OUTPUT = "kotlin.kotlin.io.output.flush();";
    public static final String GET_KOTLIN_OUTPUT = "kotlin.kotlin.io.output.buffer;";

    @NotNull
    private static final Map<EcmaVersion, ScriptableObject> versionToScope = ContainerUtil.newHashMap();

    static {
        // don't read JS, use kotlin and idea debugger ;)
        //IGNORED_JSHINT_WARNINGS.add(
        //        "Wrap an immediate function invocation in parentheses to assist the reader in understanding that the expression is the result of a function, and not the function itself.");
        //IGNORED_JSHINT_WARNINGS.add("Expected exactly one space between ';' and 'else'.");
        //// stupid jslint, see $initializer fun
        //IGNORED_JSHINT_WARNINGS.add("Do not wrap function literals in parens unless they are to be immediately invoked.");
        //// stupid jslint
        //IGNORED_JSHINT_WARNINGS.add("'_' was used before it was defined.");
        //IGNORED_JSHINT_WARNINGS.add("Empty block.");
        IGNORED_JSHINT_WARNINGS.add("Expected to see a statement and instead saw a block.");
        //IGNORED_JSHINT_WARNINGS.add("Unexpected '.'.");
        //// todo
        //IGNORED_JSHINT_WARNINGS.add("Strange loop.");
        //IGNORED_JSHINT_WARNINGS.add("Weird relation.");
        //IGNORED_JSHINT_WARNINGS.add("Weird condition.");
        //IGNORED_JSHINT_WARNINGS.add("Expected ';' and instead saw ','.");
        //IGNORED_JSHINT_WARNINGS.add("Expected an identifier and instead saw ','.");
        //// it is normal,
        //IGNORED_JSHINT_WARNINGS.add("Unexpected 'else' after 'return'.");
        IGNORED_JSHINT_WARNINGS.add("Expected ')' and instead saw 'return'.");

        //IGNORED_JSHINT_WARNINGS.add()

        // todo fix dart ast?
        //JSHINT_OPTIONS.defineProperty("white", true, ScriptableObject.READONLY);
        // vars, http://uxebu.com/blog/2010/04/02/one-var-statement-for-one-varia
        // ble/
        //JSHINT_OPTIONS.defineProperty("vars", true, ScriptableObject.READONLY);
        NativeArray globals = new NativeArray(new Object[] {"Kotlin"});
        JSHINT_OPTIONS.defineProperty("predef", globals, ScriptableObject.READONLY);
        // todo
        JSHINT_OPTIONS.defineProperty("expr", true, ScriptableObject.READONLY);
        JSHINT_OPTIONS.defineProperty("asi", true, ScriptableObject.READONLY);
        JSHINT_OPTIONS.defineProperty("laxcomma", true, ScriptableObject.READONLY);
        //JSHINT_OPTIONS.defineProperty("nomen", true, ScriptableObject.READONLY);
        //JSHINT_OPTIONS.defineProperty("continue", true, ScriptableObject.READONLY);
        //JSHINT_OPTIONS.defineProperty("plusplus", true, ScriptableObject.READONLY);
        //JSHINT_OPTIONS.defineProperty("evil", true, ScriptableObject.READONLY);

        //JSHINT_OPTIONS.defineProperty("indent", 2, ScriptableObject.READONLY);
    }

    private RhinoUtils() {
    }

    private static void runFileWithRhino(@NotNull String inputFile,
            @NotNull Context context,
            @NotNull Scriptable scope) throws Exception {
        String result;
        try {
            result = FileUtil.loadFile(new File(inputFile), CharsetToolkit.UTF8, true);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        context.setOptimizationLevel(-1);
        context.evaluateString(scope, result, inputFile, 1, null);
    }

    public static void runRhinoTest(@NotNull List<String> fileNames, @NotNull RhinoResultChecker checker) throws Exception {
        runRhinoTest(fileNames, checker, null, EcmaVersion.defaultVersion());
    }

    public static void runRhinoTest(@NotNull List<String> fileNames,
            @NotNull RhinoResultChecker checker,
            @Nullable Map<String, Object> variables,
            @NotNull EcmaVersion ecmaVersion)
            throws Exception {
        runRhinoTest(fileNames, checker, variables, ecmaVersion, Collections.<String>emptyList());
    }

    public static void runRhinoTest(@NotNull List<String> fileNames,
            @NotNull RhinoResultChecker checker,
            @Nullable Map<String, Object> variables,
            @NotNull EcmaVersion ecmaVersion,
            @NotNull List<String> jsLibraries) throws Exception {
        Context context = createContext(ecmaVersion);

        context.setOptimizationLevel(OPTIMIZATION_OFF);
        if (variables != null) {
            context.setOptimizationLevel(getOptimizationLevel(variables));
        }

        try {
            Scriptable scope = getScope(ecmaVersion, context, jsLibraries);
            putGlobalVariablesIntoScope(scope, variables);

            context.evaluateString(scope, SETUP_KOTLIN_OUTPUT, "setup kotlin output", 0, null);

            for (String filename : fileNames) {
                runFileWithRhino(filename, context, scope);
                //String problems = lintIt(context, filename, scope);
                //if (problems != null) {
                //    //fail(problems);
                //    System.out.print(problems);
                //}
            }

            finishScope(scope);
            checker.runChecks(context, scope);
        }
        finally {
            Context.exit();
        }
    }

    @NotNull
    private static Scriptable getScope(@NotNull EcmaVersion version, @NotNull Context context, @NotNull List<String> jsLibraries) {
        Scriptable parentScope = getParentScope(version, context, jsLibraries);
        Scriptable scope = context.newObject(parentScope);

        scope.put("kotlin-test", scope, parentScope.get("kotlin-test", parentScope));

        return scope;
    }

    private static void finishScope(@NotNull Scriptable scope) {
        scope.delete("kotlin-test");
    }

    @NotNull
    private static Scriptable getParentScope(@NotNull EcmaVersion version, @NotNull Context context, @NotNull List<String> jsLibraries) {
        ScriptableObject parentScope = versionToScope.get(version);
        if (parentScope == null) {
            parentScope = initScope(version, context, jsLibraries);
            versionToScope.put(version, parentScope);
        }
        return parentScope;
    }

    @NotNull
    private static ScriptableObject initScope(@NotNull EcmaVersion version, @NotNull Context context, @NotNull List<String> jsLibraries) {
        ScriptableObject scope = context.initStandardObjects();
        try {
            runFileWithRhino(DIST_DIR_JS_PATH + "kotlin.js", context, scope);
            runFileWithRhino(DIST_DIR_JS_PATH + "../classes/kotlin-test-js/kotlin-test.js", context, scope);

            //runFileWithRhino(pathToTestFilesRoot() + "jshint.js", context, scope);
            for (String jsLibrary : jsLibraries) {
                runFileWithRhino(jsLibrary, context, scope);
            }
        }
        catch (Exception e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
        //scope.sealObject();
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
    public static String getKotlinLibFile(@NotNull EcmaVersion ecmaVersion) {
        assert ecmaVersion == EcmaVersion.v5 : "Ecma 3 is deprecate";
        return KOTLIN_JS_LIB_ECMA_5;
    }

    static void flushSystemOut(@NotNull Context context, @NotNull Scriptable scope) {
        context.evaluateString(scope, FLUSH_KOTLIN_OUTPUT, "test", 0, null);
    }

    @Nullable
    private static String lintIt(Context context, String fileName, ScriptableObject scope) throws IOException {
        if (Boolean.valueOf(System.getProperty("test.lint.skip"))) {
            return null;
        }

        Object[] args = {FileUtil.loadFile(new File(fileName), true), JSHINT_OPTIONS};
        Function function = (Function) ScriptableObject.getProperty(scope.getParentScope(), "JSHINT");
        Object status = function.call(context, scope.getParentScope(), scope.getParentScope(), args);
        if (!(Boolean) Context.jsToJava(status, Boolean.class)) {
            Object errors = function.get("errors", scope);
            StringBuilder sb = new StringBuilder(fileName);
            for (Object errorObj : ((NativeArray) errors)) {
                if (errorObj == null) {
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
                    if (IGNORED_JSHINT_WARNINGS.contains(reason) ||
                        reason.startsWith("Expected exactly one space between ')' and ") ||
                        reason.startsWith("Expected '}' to match '{' from line ") ||
                        reason.startsWith("Expected '{' and instead saw ")) {
                        continue;
                    }

                    sb.append('\n').append(line).append(':').append(character).append(' ').append(reason);
                }
            }

            return sb.length() == fileName.length() ? null : sb.toString();
        }

        return null;
    }

    private static int toInt(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return -1;
    }

    private static int getOptimizationLevel(@NotNull Map<String, Object> testVariables) {
        Object optimizationLevel = testVariables.get(OPTIMIZATION_LEVEL_TEST_VARIABLE);

        if (optimizationLevel instanceof Integer) {
            return (Integer) optimizationLevel;
        }

        return OPTIMIZATION_DEFAULT;
    }
}
