/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dokkabuild.utils

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named


/**
 * Mark this [Configuration] as one that should be used to declare dependencies in
 * [org.gradle.api.Project.dependencies] block.
 *
 * Declarable Configurations should be extended by [resolvable] and [consumable] Configurations.
 * They must not have attributes.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = false
 * isCanBeDeclared = true
 * ```
 */
internal fun Configuration.declarable(
    visible: Boolean = false,
) {
    isCanBeResolved = false
    isCanBeConsumed = false
    @Suppress("UnstableApiUsage")
    isCanBeDeclared = true
    isVisible = visible
}


/**
 * Mark this [Configuration] as one that will be consumed by other subprojects.
 *
 * Consumable Configurations must extend a [declarable] Configuration.
 * They should have attributes.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = true
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.consumable(
    visible: Boolean = false,
) {
    isCanBeResolved = false
    isCanBeConsumed = true
    @Suppress("UnstableApiUsage")
    isCanBeDeclared = false
    isVisible = visible
}


/**
 * Mark this [Configuration] as one that will consume artifacts from other subprojects (also known as 'resolving')
 *
 * Resolvable Configurations should have attributes.
 *
 * ```
 * isCanBeResolved = true
 * isCanBeConsumed = false
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.resolvable(
    visible: Boolean = false,
) {
    isCanBeResolved = true
    isCanBeConsumed = false
    @Suppress("UnstableApiUsage")
    isCanBeDeclared = false
    isVisible = visible
}


internal fun AttributeContainer.jvmJar(objects: ObjectFactory) {
    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.STANDARD_JVM))
    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
}
