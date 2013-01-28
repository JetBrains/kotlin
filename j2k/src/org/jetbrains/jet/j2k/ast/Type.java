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

package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

public abstract class Type extends Element {
    @NotNull
    public static final Type EMPTY_TYPE = new EmptyType();
    boolean myNullable = true;

    @NotNull
    @Override
    public Kind getKind() {
        return Kind.TYPE;
    }

    @NotNull
    public Type convertedToNotNull() {
        myNullable = false;
        return this;
    }

    public boolean isNullable() {
        return myNullable;
    }

    String isNullableStr() {
        return isNullable() ? QUEST : EMPTY;
    }

    private static class EmptyType extends Type {
        @NotNull
        @Override
        public String toKotlin() {
            return "UNRESOLVED_TYPE";
        }

        @Override
        public boolean isNullable() {
            return false;
        }
    }
}
