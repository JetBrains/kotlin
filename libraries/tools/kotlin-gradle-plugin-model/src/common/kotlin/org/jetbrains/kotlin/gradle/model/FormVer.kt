/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

interface FormVer {
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
     * Return the desired Viper log level.
     *
     * @return the Viper log level
     */
    val logLevel: String?

    /**
     * Returns the desired behaviour when encountering unsupported Kotlin features.
     *
     * @return the behaviour for unsupported features
     */
    val unsupportedFeatureBehaviour: String?

    /**
     * Returns the choice of targets to convert to Viper.
     *
     * @return the choice of targets
     */
    val conversionTargetsSelection: String?

    /**
     * Returns the choice of targets to verify.
     *
     * @return the choice of targets
     */
    val verificationTargetsSelection: String?
}