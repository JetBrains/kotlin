/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import java.io.Serializable

/**
 * Wraps models of a given type for all different projects inside a Gradle multi project.
 */
class ModelContainer<T> : Serializable {
    // Key is the project path
    private val modelContainer = HashMap<String, T>()

    fun addModel(path: String, model: T) {
        modelContainer[path] = model
    }

    fun getModel(path: String): T? {
        return modelContainer[path]
    }

    fun hasModel(path: String): Boolean {
        return modelContainer[path] != null
    }
}