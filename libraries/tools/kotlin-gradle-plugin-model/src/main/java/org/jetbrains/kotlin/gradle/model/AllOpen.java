/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Entry point for Kotlin All Open models.
 * Represents the description of annotations interpreted by 'kotlin-allopen' plugin.
 */
public interface AllOpen {

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
     * Return the list of annotations.
     *
     * @return the list of annotations.
     */
    @NotNull
    List<String> getAnnotations();

    /**
     * Return the list of presets.
     *
     * @return the list of presets.
     */
    @NotNull
    List<String> getPresets();
}
