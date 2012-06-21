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
import com.intellij.openapi.util.io.FileUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.test.BasicTest;
import org.mozilla.javascript.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Pavel Talanov
 */
public final class RhinoUtils {
    private static final Set<String> IGNORED_JSLINT_WARNINGS = new THashSet<String>();

    static {
        // todo dart ast bug
        IGNORED_JSLINT_WARNINGS.add("Unexpected space between '}' and '('.");
        // don't read JS, use kotlin and idea debugger ;)
        IGNORED_JSLINT_WARNINGS.add("Wrap an immediate function invocation in parentheses to assist the reader in understanding that the expression is the result of a function, and not the function itself.");
    }

    private static final RhinoFunctionManager functionManager = new RhinoFunctionManager(
            new Supplier<String>() {
                @Override
                public String get() {
                    return fileToString(BasicTest.JSLINT_LIB);
                }
            },
            "JSLINT"
    );

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
        context.evaluateString(scope, fileToString(inputFile), inputFile, 1, null);
    }

    public static void runRhinoTest(@NotNull List<String> fileNames,
                                    @NotNull RhinoResultChecker checker) throws Exception {

        runRhinoTest(fileNames, checker, null, EcmaVersion.defaultVersion());
    }

    public static void runRhinoTest(@NotNull List<String> fileNames,
                                    @NotNull RhinoResultChecker checker,
                                    @Nullable Map<String, Object> variables,
                                    @NotNull EcmaVersion ecmaVersion) throws Exception {
        Context context = Context.enter();
        if (ecmaVersion == EcmaVersion.v5) {
            // actually, currently, doesn't matter because dart doesn't produce js 1.8 code (expression closures)
            context.setLanguageVersion(Context.VERSION_1_8);
        }

        Scriptable scope = context.initStandardObjects();
        if (variables != null) {
            Set<Map.Entry<String,Object>> entries = variables.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                String name = entry.getKey();
                Object value = entry.getValue();
                scope.put(name, scope, value);
            }
        }
        for (String filename : fileNames) {
            runFileWithRhino(filename, context, scope);
        }
        checker.runChecks(context, scope);

        lintIt(context, fileNames.get(fileNames.size() - 1));

        Context.exit();
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
        NativeArray globals = new NativeArray(new Object[]{"Kotlin"});
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
