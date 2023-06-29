/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.mpp.includeSources
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.sourcesJarTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

/**
 *
 * Will add all sources (including transitive dependsOn edges) from the compilation into this jar task given
 * the multiplatform convention.
 *
 * e.g.
 * ```
 * src/commonMain/kotlin/CommonMain.kt
 * src/jvmMain/kotlin/JvmMain.kt
 * ```
 *
 * will be packaged like
 * ```
 * /commonMain/CommonMain.kt
 * /jvmMain/JvmMain.kt
 * ```
 *
 * @since 1.9.20
 */
@ExternalKotlinTargetApi
fun Jar.includeSources(compilation: DecoratedExternalKotlinCompilation) {
    includeSources(compilation.internal)
}

/**
 * Registers, or returns if already existing, a sources jar task that contains
 * all sources of the given compilation (see [includeSources])
 *
 * @since 1.9.20
 */
@ExternalKotlinTargetApi
fun DecoratedExternalKotlinTarget.sourcesJarTask(compilation: DecoratedExternalKotlinCompilation): TaskProvider<Jar> {
    return sourcesJarTask(
        compilation,
        componentName = lowerCamelCaseName(targetName, compilation.name.takeUnless { compilation.isMain() }),
        artifactNameAppendix = targetName.toLowerCaseAsciiOnly()
    )
}

/**
 * Publishes the sources packaged by the given [jarTask] in this targets' publication.
 *
 * @since 1.9.20
 */
@ExternalKotlinTargetApi
fun DecoratedExternalKotlinTarget.publishSources(jarTask: TaskProvider<Jar>) {
    delegate.sourcesElementsPublishedConfiguration.outgoing.artifact(jarTask) { artifact ->
        artifact.classifier = "sources"
    }
}

/**
 * Publishes the sources associated with the given [compilation] in this targets' publication.
 * Will register the corresponding [sourcesJarTask] if necessary.
 *
 * @since 1.9.20
 */
@ExternalKotlinTargetApi
fun DecoratedExternalKotlinTarget.publishSources(compilation: DecoratedExternalKotlinCompilation) {
    publishSources(sourcesJarTask(compilation))
}
