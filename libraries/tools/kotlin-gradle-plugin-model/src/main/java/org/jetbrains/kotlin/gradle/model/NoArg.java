/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Entry point for Kotlin No Arg models.
 * Represents the description of annotations interpreted by 'kotlin-noarg' plugin.
 */
public interface NoArg {

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

    /**
     * Return if should invoke initializers.
     * Only makes sense for type NO_ARG.
     *
     * @return if initializers should be invoked.
     */
    boolean isInvokeInitializers();
}
