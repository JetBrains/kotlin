/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.utils;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arguments format: ((namedArg|positionalArg)\s+)*`
 * <p>
 * Where: namedArg -- 'key=value' or 'key="spaced value"'
 * positionalArg -- 'value'
 * <p>
 * Neither key, nor value should contain spaces.
 */
public class ArgumentsHelper {
    private final List<String> positionalArguments = new ArrayList<>();
    private final Map<String, String> namedArguments = new HashMap<>();
    private final String entry;
    private final Pattern argumentsPattern = Pattern.compile("[\\w$_;\\.]+(=((\".*?\")|[\\w$_;\\.]+))?");
    final File sourceFile;

    ArgumentsHelper(@NotNull String directiveEntry, @NotNull File directiveSourceFile) {
        entry = directiveEntry;
        sourceFile = directiveSourceFile;

        Matcher matcher = argumentsPattern.matcher(directiveEntry);

        while (matcher.find()) {
            String argument = matcher.group();
            String[] keyVal = argument.split("=");
            switch (keyVal.length) {
                case 1:
                    positionalArguments.add(keyVal[0]);
                    break;
                case 2:
                    String value = keyVal[1];
                    if (value.charAt(0) == '"') {
                        value = value.substring(1, value.length() - 1);
                    }
                    namedArguments.put(keyVal[0], value);
                    break;
                default:
                    throw new AssertionError("Wrong argument format: " + argument);
            }
        }
    }

    @NotNull
    String getFirst() {
        return getPositionalArgument(0);
    }

    @NotNull
    String getPositionalArgument(int index) {
        assert positionalArguments.size() > index : "Argument at index `" + index + "` not found in entry: " + entry;
        return positionalArguments.get(index);
    }

    @NotNull
    String getNamedArgument(@NotNull String name) {
        assert namedArguments.containsKey(name) : "Argument `" + name + "` not found in entry: " + entry;
        return namedArguments.get(name);
    }

    @Nullable
    String findNamedArgument(@NotNull String name) {
        return namedArguments.get(name);
    }

    @NotNull
    List<String> findNamedListArgument(@NotNull String name) {
        String value = findNamedArgument(name);
        if (value == null) return Collections.emptyList();
        return StringUtil.split(value, ";");
    }
}
