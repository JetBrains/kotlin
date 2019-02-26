/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions;

import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtScript;

public class ScriptPriorities {

    public static final Key<Integer> PRIORITY_KEY = Key.create(KtScript.class.getName() + ".priority");

    public static int getScriptPriority(@NotNull KtScript script) {
        Integer priority = script.getUserData(PRIORITY_KEY);
        return priority == null ? 0 : priority;
    }
}
