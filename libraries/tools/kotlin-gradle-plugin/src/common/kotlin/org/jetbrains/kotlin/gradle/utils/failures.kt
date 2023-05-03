/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.tooling.model.kotlin.dsl.KotlinDslModelsParameters
import org.jetbrains.kotlin.gradle.plugin.internal.configurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.usedAtConfigurationTime

/**
 * Returns *all* failures that have already happened during project configuration.
 * This property will respect a special mode called 'ClasspathMode' which is triggered by the IDE.
 * In this mode, Gradle will catch all exceptions and put it into a special 'Collector'.
 * This exceptions will also be available in the returned list.
 *
 * In regular (non 'ClasspathMode'), the returned list is expected to have only one or zero elements.
 */
internal val Project.failures: List<Throwable>
    get() {
        /* Respecting special mode in IDE that catches exceptions and collects them outside of Project.state.failure */
        val failuresFromIdeaSyncClasspathMode = if (ideaSyncClasspathModeUtil.isClasspathModeActive) ideaSyncClasspathModeUtil.exceptions
        else emptyList()

        val regularFailures = listOfNotNull(project.state.failure)
        return regularFailures + failuresFromIdeaSyncClasspathMode
    }


/**
 * Special Mode only active during IDEA sync (using the 'classpath button' instead of the 'reload button').
 * In this ide sync mode, Gradle is configured to actually catch exceptions during the buildscript evaluation.
 * Those exceptions will be collected into a special Gradle service.
 *
 * Accessing those exceptions here via internal APIs is necessary, because afterEvaluate based hooks
 * are still executed. The should *not* run if the project is in a 'bad' failure state.
 *
 * @see runProjectConfigurationHealthCheck
 * @see runProjectConfigurationHealthCheckWhenEvaluated
 */
private val Project.ideaSyncClasspathModeUtil
    get() = object {
        val isClasspathModeActive: Boolean
            /*
            ConfigurationTimePropertiesAccessorVariantFactory type is not known for plugin variants.
            Be lenient in cases where the factory is not available (e.g. functionalTests where just a blank project is used)
             */
            get() = runCatching {
                providers
                    .systemProperty(KotlinDslModelsParameters.PROVIDER_MODE_SYSTEM_PROPERTY_NAME)
                    .usedAtConfigurationTime(configurationTimePropertiesAccessor)
                    .orNull == KotlinDslModelsParameters.CLASSPATH_MODE_SYSTEM_PROPERTY_VALUE
            }.getOrElse { failure ->
                logger.error("Failed to access '${KotlinDslModelsParameters.PROVIDER_MODE_SYSTEM_PROPERTY_NAME}'", failure)
                false
            }

        val exceptions: List<Exception>
            get() {
                try {
                    val classPathModeExceptionCollectionClass = Class.forName(
                        "org.gradle.kotlin.dsl.provider.ClassPathModeExceptionCollector"
                    )
                    val exceptionCollector = (project as ProjectInternal).services.get(classPathModeExceptionCollectionClass)

                    @Suppress("unchecked_cast")
                    return classPathModeExceptionCollectionClass.methods
                        .first { it.name == "getExceptions" }
                        .invoke(exceptionCollector) as List<Exception>
                } catch (t: Throwable) {
                    logger.error("Failed to access 'ClassPathModeExceptionCollector'", t)
                    return emptyList()
                }
            }
    }
