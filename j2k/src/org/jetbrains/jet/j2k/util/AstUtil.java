/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.ast.INode;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AstUtil {
    private AstUtil() {
    }

    private static String join(@NotNull String[] array, @Nullable String delimiter) {
        StringBuilder buffer = new StringBuilder();
        boolean haveDelimiter = (delimiter != null);

        for (int i = 0; i < array.length; i++) {
            buffer.append(array[i]);

            if (haveDelimiter && (i + 1) < array.length) {
                buffer.append(delimiter);
            }
        }

        return buffer.toString();
    }

    public static String joinNodes(@NotNull List<? extends INode> nodes, String delimiter) {
        return join(nodesToKotlin(nodes), delimiter);
    }

    public static String join(@NotNull List<String> array, String delimiter) {
        return join(array.toArray(new String[array.size()]), delimiter);
    }

    @NotNull
    public static List<String> nodesToKotlin(@NotNull List<? extends INode> nodes) {
        List<String> result = new LinkedList<String>();
        for (INode n : nodes)
            result.add(n.toKotlin());
        return result;
    }

    @NotNull
    public static String upperFirstCharacter(@NotNull String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    @NotNull
    public static String lowerFirstCharacter(@NotNull String string) {
        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }

    @NotNull
    public static <T> List<String> createListWithEmptyString(@NotNull List<T> arguments) {
        List<String> conversions = new LinkedList<String>();
        //noinspection UnusedDeclaration
        for (T argument : arguments) conversions.add("");
        return conversions;
    }

    @NotNull
    public static List<String> applyConversions(@NotNull List<String> first, @NotNull List<String> second) {
        List<String> result = new LinkedList<String>();
        assert first.size() == second.size() : "Lists must have the same size.";
        for (int i = 0; i < first.size(); i++) {
            result.add(applyConversionForOneItem(first.get(i), second.get(i)));
        }
        return result;
    }

    @NotNull
    public static String applyConversionForOneItem(@NotNull String f, @NotNull String s) {
        if (s.isEmpty()) {
            return f;
        }
        else {
            return "(" + f + ")" + s;
        }
    }

    @NotNull
    public static <T> T getOrElse(@NotNull Map<T, T> map, @NotNull T e, @NotNull T orElse) {
        if (map.containsKey(e)) {
            return map.get(e);
        }
        return orElse;
    }

    @NotNull
    public static String replaceLastQuest(@NotNull String str) {
        if (str.endsWith("?")) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }
}
