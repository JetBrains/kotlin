/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.test.utils;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static org.jetbrains.k2js.inline.util.UtilPackage.collectNamedFunctions;

public class AstSearchUtil {
    @NotNull
    public static JsFunction getFunction(@NotNull JsNode searchRoot, String name) {
        Map<JsName, JsFunction> functions = collectNamedFunctions(searchRoot);

        for (Map.Entry<JsName, JsFunction> entry : functions.entrySet()) {
            if (entry.getKey().getIdent().equals(name)) {
                return entry.getValue();
            }
        }

        throw new AssertionError("Function `" + name + "` was not found");
    }
}
