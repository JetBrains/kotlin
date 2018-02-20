/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.jps.builders.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.java.dependencyView.Callbacks;
import org.jetbrains.jps.incremental.CompileContext;

/**
 * A "copy" of Intellij service unavailable in current builds,
 * so our plugin can be compiled against it (API is not yet merged in Intellij,
 * even then it would be available only in 182.* builds).
 *
 * // todo: remove when compatibility layers will be finished
 */
@SuppressWarnings("unused")
public interface ConstantSearchProvider {
    @NotNull
    Callbacks.ConstantAffectionResolver getConstantSearch(@NotNull CompileContext context);
}
