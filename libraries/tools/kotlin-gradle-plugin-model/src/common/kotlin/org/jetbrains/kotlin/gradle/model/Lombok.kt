/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import java.io.File

interface Lombok {

    /**
     * @return Return a number representing the version of this API.
     * Always increasing if changed.
     */
    val modelVersion: Long

    /**
     * @return the module (Gradle project) name
     */
    val name: String

    /**
     * Lombok configuration file
     */
    val configurationFile: File?
}
