/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model;

import org.jetbrains.annotations.Nullable;

/**
 * Wraps all experimental features information for a given Kotlin project.
 * @see KotlinProject
 */
public interface ExperimentalFeatures {

    /**
     * Return coroutines string.
     *
     * @return coroutines.
     */
    @Nullable
    String getCoroutines();
}
