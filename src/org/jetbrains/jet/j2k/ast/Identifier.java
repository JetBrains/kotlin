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

/**
 * @author ignatov
 */
public class Identifier extends Expression  {
    @NotNull
    public static Identifier EMPTY_IDENTIFIER = new Identifier("");

    private final String myName;
    private boolean myIsNullable = true;
    private boolean myQuotingNeeded = true;

    public Identifier(String name) {
        myName = name;
    }

    public Identifier(String name, boolean isNullable) {
        myName = name;
        myIsNullable = isNullable;
    }

    public Identifier(String name, boolean isNullable, boolean quotingNeeded) {
        myName = name;
        myIsNullable = isNullable;
        myQuotingNeeded = quotingNeeded;
    }

    public boolean isEmpty() {
        return myName.length() == 0;
    }

    public String getName() {
        return myName;
    }

    @NotNull
    private static String quote(String str) {
        return BACKTICK + str + BACKTICK;
    }

    @Override
    public boolean isNullable() {
        return myIsNullable;
    }

    private String ifNeedQuote() {
        if (myQuotingNeeded && (ONLY_KOTLIN_KEYWORDS.contains(myName) || myName.contains("$"))) {
            return quote(myName);
        }
        return myName;
    }

    @NotNull
    @Override
    public String toKotlin() {
        return ifNeedQuote();
    }
}
