/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

internal fun AbstractKotlinCompile<*>.nagUserFreeArgsModifiedOnExecution(
    freeArgsValue: List<String>
) {
    if (!suppressKotlinOptionsFreeArgsModificationWarning.get()) {
        // Trying to approximately filter out KGP and non-related Gradle/Java/Groovy classes from stacktrace
        val modificationStacktrace = Thread.currentThread().stackTrace
            .asSequence()
            .filter {
                !it.className.startsWith("org.jetbrains.kotlin.gradle") &&
                        !it.className.startsWith("org.gradle.api") &&
                        !it.className.startsWith("org.gradle.internal") &&
                        !it.className.startsWith("org.gradle.execution") &&
                        !it.className.startsWith("java.") &&
                        !it.className.startsWith("sun.") &&
                        !it.className.startsWith("groovy.") &&
                        !it.className.startsWith("kotlin.") &&
                        !it.className.startsWith("org.codehaus.groovy.")
            }
            .takeWhile {
                !it.className.startsWith("org.gradle.composite") &&
                        !it.className.startsWith("org.gradle.configuration.internal")
            }
            .joinToString(separator = "\n") {
                "    $it"
            }

        logger.warn(
            "kotlinOptions.freeCompilerArgs were changed on task $path execution phase: ${freeArgsValue.joinToString()}\n" +
                    "This behaviour is deprecated and become an error in future releases!\n" +
                    "Approximate place of modification at execution phase:\n" +
                    modificationStacktrace
        )
    }
}
