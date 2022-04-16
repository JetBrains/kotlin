/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

/**
 * Entry point for Kotlin All Open models.
 * Represents the description of annotations interpreted by 'kotlin-allopen' plugin.
 */
interface AllOpen {

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
     * Return the list of annotations.
     *
     * @return the list of annotations.
     */
    val annotations: List<String>

    /**
     * Return the list of presets.
     *
     * @return the list of presets.
     */
    val presets: List<String>
}