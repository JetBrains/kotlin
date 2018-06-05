/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Represents a source set for a given kapt model.
 * @see Kapt
 */
public interface KaptSourceSet {

    /**
     * Possible source set types.
     */
    enum KaptSourceSetType {
        PRODUCTION,
        TEST
    }

    /**
     * Return the source set name.
     *
     * @return the source set name.
     */
    @NotNull
    String getName();

    /**
     * Return the type of the source set.
     *
     * @return the type of the source set.
     */
    @NotNull
    KaptSourceSetType getType();

    /**
     * Return generated sources directory.
     *
     * @return generated sources directory.
     */
    @NotNull
    File getGeneratedSourcesDirectory();

    /**
     * Return Kotlin generated sources directory.
     *
     * @return Kotlin generated sources directory.
     */
    @NotNull
    File getGeneratedKotlinSourcesDirectory();

    /**
     * Return Kotlin generated classes directory.
     *
     * @return Kotlin generated classes directory.
     */
    @NotNull
    File getGeneratedClassesDirectory();
}
