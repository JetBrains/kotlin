/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

/**
 * Entry point for annotation processor model.
 * Plugin 'kotlin-kapt' can produce this model.
 */
interface Kapt {

    /**
     * Return a number representing the version of this API.
     * Always increasing if changed.
     *
     * @return the version of this model.
     */
    val modelVersion: Long

    /**
     * Returns the module (Gradle project) name.
     *
     * @return the module name.
     */
    val name: String

    /**
     * Return all kapt source sets.
     *
     * @return all kapt source sets.
     */
    val kaptSourceSets: Collection<KaptSourceSet>
}