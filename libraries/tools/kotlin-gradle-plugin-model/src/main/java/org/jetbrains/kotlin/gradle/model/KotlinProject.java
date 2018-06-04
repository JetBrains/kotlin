/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Entry point for the model of Kotlin Projects.
 * Plugins 'kotlin', 'kotlin-platform-jvm', 'kotlin2js', 'kotlin-platform-js' and 'kotlin-platform-common' can produce this model.
 */
public interface KotlinProject {

    /**
     * Possible Kotlin project types.
     */
    enum ProjectType {

        /** Indicator of platform plugin id 'kotlin-platform-jvm' or 'kotlin'. */
        PLATFORM_JVM,

        /** Indicator of platform plugin id 'kotlin-platform-js' or 'kotlin2js'. */
        PLATFORM_JS,

        /** Indicator of platform plugin id 'kotlin-platform-common'. */
        PLATFORM_COMMON
    }

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
     * Return the Kotlin version.
     *
     * @return the Kotlin version.
     */
    @NotNull
    String getKotlinVersion();

    /**
     * Return the type of the platform plugin applied.
     *
     * @return the type of the platform plugin applied. Possible values are defined in the enum.
     */
    @NotNull
    ProjectType getProjectType();

    /**
     * Return all source sets used by Kotlin.
     *
     * @return all source sets.
     */
    @NotNull
    Collection<SourceSet> getSourceSets();

    /**
     * Return all modules (Gradle projects) registered as 'expectedBy' dependency.
     *
     * @return expectedBy dependencies.
     */
    @NotNull
    Collection<String> getExpectedByDependencies();

    /**
     * Return an object containing a descriptor of the experimental features.
     *
     * @return experimental features.
     */
    @NotNull
    ExperimentalFeatures getExperimentalFeatures();
}
