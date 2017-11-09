/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.maven.kapt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class KaptOption {
    @NotNull
    private final String key;

    @NotNull
    private final String value;

    KaptOption(@NotNull String key, boolean value) {
        this(key, String.valueOf(value));
    }

    KaptOption(@NotNull String key, @Nullable String[] value) {
        this(key, renderStringArray(value));
    }

    KaptOption(@NotNull String key, @Nullable String value) {
        this.key = key;
        this.value = String.valueOf(value);
    }

    @NotNull
    private static String renderStringArray(@Nullable String[] arr) {
        if (arr == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (String s : arr) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(s);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "plugin:org.jetbrains.kotlin.kapt3:" + key + "=" + value;
    }
}
