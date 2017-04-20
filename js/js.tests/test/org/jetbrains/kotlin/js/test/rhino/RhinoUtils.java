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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.js.test.BasicBoxTest.DIST_DIR_JS_PATH;

public final class RhinoUtils {

    private static final int OPTIMIZATION_OFF = -1;

    private static final String SETUP_KOTLIN_OUTPUT = "kotlin.kotlin.io.output = new kotlin.kotlin.io.BufferedOutput();";
    private static final String FLUSH_KOTLIN_OUTPUT = "kotlin.kotlin.io.output.flush();";
    public static final String GET_KOTLIN_OUTPUT = "kotlin.kotlin.io.output.buffer;";

    @NotNull
    private static final Map<EcmaVersion, ScriptableObject> versionToScope = ContainerUtil.newHashMap();

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
        EcmaVersion ecmaVersion = EcmaVersion.defaultVersion();
        Context context = createContext(ecmaVersion);

        context.setOptimizationLevel(OPTIMIZATION_OFF);

        try {
            Scriptable scope = getScope(ecmaVersion, context);

            context.evaluateString(scope, SETUP_KOTLIN_OUTPUT, "setup kotlin output", 0, null);

            for (String filename : fileNames) {
                runFileWithRhino(filename, context, scope);
            }

            finishScope(scope);
            checker.runChecks(context, scope);
        }
        finally {
            Context.exit();
        }
    }

    @NotNull
    private static Scriptable getScope(@NotNull EcmaVersion version, @NotNull Context context) {
        Scriptable parentScope = getParentScope(version, context);
        Scriptable scope = context.newObject(parentScope);

        scope.put("kotlin-test", scope, parentScope.get("kotlin-test", parentScope));

        return scope;
    }

    private static void finishScope(@NotNull Scriptable scope) {
        scope.delete("kotlin-test");
    }

    @NotNull
    private static Scriptable getParentScope(@NotNull EcmaVersion version, @NotNull Context context) {
        return versionToScope.computeIfAbsent(version, v -> initScope(context));
    }

    @NotNull
    private static ScriptableObject initScope(@NotNull Context context) {
        ScriptableObject scope = context.initStandardObjects();
        try {
            runFileWithRhino(DIST_DIR_JS_PATH + "../../js/js.translator/testData/rhino-polyfills.js", context, scope);
            runFileWithRhino(DIST_DIR_JS_PATH + "kotlin.js", context, scope);
            runFileWithRhino(DIST_DIR_JS_PATH + "kotlin-test.js", context, scope);
            context.evaluateString(scope,
                                   "this['kotlin-test'].kotlin.test.overrideAsserter_wbnzx$(new this['kotlin-test'].kotlin.test.DefaultAsserter());",
                                   "change asserter to DefaultAsserter", 1, null);
        }
        catch (Exception e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
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

    static void flushSystemOut(@NotNull Context context, @NotNull Scriptable scope) {
        context.evaluateString(scope, FLUSH_KOTLIN_OUTPUT, "test", 0, null);
    }
}
