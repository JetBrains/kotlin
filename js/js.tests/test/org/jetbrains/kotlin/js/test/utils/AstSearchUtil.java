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

package org.jetbrains.kotlin.js.test.utils;

import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsFunction;
import org.jetbrains.kotlin.js.backend.ast.JsName;
import org.jetbrains.kotlin.js.backend.ast.JsNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.inline.util.CollectUtilsKt;

import java.util.Map;

import static org.jetbrains.kotlin.js.inline.util.CollectUtilsKt.collectNamedFunctions;

public class AstSearchUtil {
    @NotNull
    public static JsFunction getFunction(@NotNull JsNode searchRoot, String name) {
        JsFunction function = findByIdent(collectNamedFunctions(searchRoot), name);
        assert function != null: "Function `" + name + "` was not found";
        return function;
    }

    @NotNull
    public static JsExpression getMetadataOrFunction(@NotNull JsNode searchRoot, @NotNull String name) {
        JsExpression property = findByIdent(CollectUtilsKt.collectNamedFunctionsOrMetadata(searchRoot), name);
        assert property != null: "Property `" + name + "` was not found";
        return property;
    }

    @Nullable
    private static <T extends JsExpression> T findByIdent(@NotNull Map<JsName, T> properties, @NotNull String name) {
        for (Map.Entry<JsName, T> entry : properties.entrySet()) {
            if (entry.getKey().getIdent().equals(name)) {
                return entry.getValue();
            }
        }

        return null;
    }
}
