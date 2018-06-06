/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entry point for Kotlin Android Extensions models.
 * Represents the description of Android only features. Provided by 'kotlin-android-extensions' plugin.
 */
public interface KotlinAndroidExtension {

    /**
     * Return a number representing the version of this API.
     * Always increasing if changed.
     *
     * @return the version of this model.
     */
    long getModelVersion();

    /**
     * Returns the module (Gradle project) name.
     *
     * @return the module name.
     */
    @NotNull
    String getName();

    /**
     * Indicate the use of experimental features.
     *
     * @return if experimental features are used.
     */
    boolean isExperimental();

    /**
     * Return the default cache implementation.
     *
     * @return the default cache implementation.
     */
    @Nullable
    String getDefaultCacheImplementation();
}
