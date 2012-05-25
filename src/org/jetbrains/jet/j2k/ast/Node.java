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

package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author ignatov
 */
public abstract class Node implements INode {
    @NotNull
    final static Set<String> ONLY_KOTLIN_KEYWORDS = new HashSet<String>(Arrays.asList(
            "package", "as", "type", "val", "var", "fun", "is", "in", "object", "when", "trait", "This"
    ));

    @NotNull
    public final static Set<String> PRIMITIVE_TYPES = new HashSet<String>(Arrays.asList(
            "double", "float", "long", "int", "short", "byte", "boolean", "char"
    ));

    static final String N = "\n";
    @NotNull
    static final String N2 = N + N;
    @NotNull
    static final String SPACE = " ";
    @NotNull
    static final String EQUAL = "=";
    @NotNull
    static final String EMPTY = "";
    @NotNull
    static final String DOT = ".";
    @NotNull
    static final String QUESTDOT = "?.";
    @NotNull
    static final String COLON = ":";
    @NotNull
    static final String IN = "in";
    @NotNull
    static final String AT = "@";
    @NotNull
    static final String BACKTICK = "`";
    @NotNull
    static final String QUEST = "?";
    @NotNull
    public static final String COMMA_WITH_SPACE = "," + SPACE;
    @NotNull
    static final String STAR = "*";
    @NotNull
    protected static final String ZERO = "0";
}
