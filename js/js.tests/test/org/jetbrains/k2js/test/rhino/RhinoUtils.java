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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.config.EcmaVersion;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Pavel Talanov
 */
public final class RhinoUtils {

    private RhinoUtils() {

    }

    private static void runFileWithRhino(@NotNull String inputFile,
                                         @NotNull Context context,
                                         @NotNull Scriptable scope) throws Exception {
        FileReader reader = new FileReader(inputFile);
        try {
            context.evaluateReader(scope, reader, inputFile, 1, null);
        } finally {
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
        Context.exit();
    }
}
