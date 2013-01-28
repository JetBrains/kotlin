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

import java.util.Set;

public abstract class Member extends Node implements IMember {
    Set<String> myModifiers;

    @NotNull
    String accessModifier() {
        for (String m : myModifiers)
            if (m.equals(Modifier.PUBLIC) || m.equals(Modifier.PROTECTED) || m.equals(Modifier.PRIVATE)) {
                return m;
            }
        return EMPTY; // package local converted to internal, but we use internal by default
    }

    @Override
    public boolean isAbstract() {
        return myModifiers.contains(Modifier.ABSTRACT);
    }

    @Override
    public boolean isStatic() {
        return myModifiers.contains(Modifier.STATIC);
    }
}
