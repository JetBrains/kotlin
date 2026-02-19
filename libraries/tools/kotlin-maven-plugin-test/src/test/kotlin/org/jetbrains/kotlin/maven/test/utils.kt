/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import org.junit.jupiter.api.extension.ExtensionContext
import java.nio.file.Path

// make sure it is file:// + absolute canonical path
fun Path.toCanonicalLocalFileUrlString(): String {
    return "file://" + toFile().canonicalPath
}

val isWindowsHost get() = System.getProperty("os.name").lowercase().contains("windows")

val isTeamCityRun = System.getenv("TEAMCITY_VERSION") != null

inline fun <reified T : Annotation> findAnnotationOrNull(context: ExtensionContext): T? {
    var nextSuperclass: Class<*>? = context.testClass.get().superclass
    val superClassSequence = if (nextSuperclass != null) {
        generateSequence {
            val currentSuperclass = nextSuperclass
            nextSuperclass = nextSuperclass?.superclass
            currentSuperclass
        }
    } else {
        emptySequence()
    }

    return sequenceOf(
        context.testMethod.orElse(null),
        context.testClass.orElse(null)
    )
        .filterNotNull()
        .plus(superClassSequence)
        .mapNotNull { declaration ->
            declaration.annotations.firstOrNull { it is T }
        }
        .firstOrNull() as T?
        ?: context.testMethod.get().annotations
            .mapNotNull { annotation ->
                annotation.annotationClass.annotations.firstOrNull { it is T }
            }
            .firstOrNull() as T?
}