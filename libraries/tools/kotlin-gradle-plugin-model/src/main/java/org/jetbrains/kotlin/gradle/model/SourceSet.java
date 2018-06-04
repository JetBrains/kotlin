/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;

/**
 * Represents a source set for a given Kotlin Gradle project.
 * @see KotlinProject
 */
public interface SourceSet {

    /**
     * Possible source set types.
     */
    enum SourceSetType {
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
    SourceSetType getType();

    /**
     * Return the names of all friend source sets.
     *
     * @return friend source sets.
     */
    @NotNull
    Collection<String> getFriendSourceSets();

    /**
     * Return all Kotlin sources directories.
     *
     * @return all Kotlin sources directories.
     */
    @NotNull
    Collection<File> getSourceDirectories();

    /**
     * Return all Kotlin resources directories.
     *
     * @return all Kotlin resources directories.
     */
    @NotNull
    Collection<File> getResourcesDirectories();

    /**
     * Return the classes output directory.
     *
     * @return the classes output directory.
     */
    @NotNull
    File getClassesOutputDirectory();

    /**
     * Return the resources output directory.
     *
     * @return the resources output directory.
     */
    @NotNull
    File getResourcesOutputDirectory();

    /**
     * Return an object containing all compiler arguments for this source set.
     *
     * @return compiler arguments for this source set.
     */
    @NotNull
    CompilerArguments getCompilerArguments();
}
