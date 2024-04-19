/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle.testUtils

import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.internal.service.DefaultServiceRegistry
import org.gradle.internal.service.scopes.ProjectScopeServices
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.tooling.events.OperationCompletionListener
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradleSubplugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicReference

fun buildProject(
    projectBuilder: ProjectBuilder.() -> Unit = { },
    configureProject: Project.() -> Unit = {},
): ProjectInternal = ProjectBuilder
    .builder()
    .apply(projectBuilder)
    .build()
    //temporary solution for BuildEventsListenerRegistry
    .also { addBuildEventsListenerRegistryMock(it) }
    .also { disableDownloadingKonanFromMavenCentral(it) }
    .apply(configureProject)
    .let { it as ProjectInternal }

fun buildProjectWithJvm(
    projectBuilder: ProjectBuilder.() -> Unit = {},
    preApplyCode: Project.() -> Unit = {},
    code: Project.() -> Unit = {}
) = buildProject(projectBuilder) {
    preApplyCode()
    project.applyKotlinJvmPlugin()
    project.applyKotlinComposePlugin()
    code()
}

fun buildProjectWithMPP(
    projectBuilder: ProjectBuilder.() -> Unit = { },
    preApplyCode: Project.() -> Unit = {},
    code: Project.() -> Unit = {}
) = buildProject(projectBuilder) {
    preApplyCode()
    project.applyMultiplatformPlugin()
    project.applyKotlinComposePlugin()
    code()
}

/**
 * In Gradle 6.7-rc-1 BuildEventsListenerRegistry service is not created in we need it in order
 * to instantiate AGP. This creates a fake one and injects it - http://b/168630734.
 * https://github.com/gradle/gradle/issues/16774 (Waiting for Gradle 7.5)
 */
internal fun addBuildEventsListenerRegistryMock(project: Project) {
    val executedExtensionKey = "addBuildEventsListenerRegistryMock.executed"
    try {
        if (project.findExtension<Boolean>(executedExtensionKey) == true) return
        val projectScopeServices = (project as DefaultProject).services as ProjectScopeServices
        val state: Field = ProjectScopeServices::class.java.superclass.getDeclaredField("state")
        state.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateValue: AtomicReference<Any> = state.get(projectScopeServices) as AtomicReference<Any>
        val enumClass = Class.forName(DefaultServiceRegistry::class.java.name + "\$State")
        stateValue.set(enumClass.enumConstants[0])

        // add service and set state so that future mutations are not allowed
        projectScopeServices.add(BuildEventsListenerRegistry::class.java, BuildEventsListenerRegistryMock)
        stateValue.set(enumClass.enumConstants[1])
        project.addExtension(executedExtensionKey, true)
    } catch (e: Throwable) {
        throw RuntimeException(e)
    }
}

object BuildEventsListenerRegistryMock : BuildEventsListenerRegistry {
    override fun onTaskCompletion(listener: Provider<out OperationCompletionListener>?) {
    }
}

internal inline fun <reified T : Any> Any.findExtension(name: String): T? =
    (this as ExtensionAware).extensions.findByName(name)?.let { it as T? }

internal inline fun <reified T : Any> Any.addExtension(name: String, extension: T) =
    (this as ExtensionAware).extensions.add(T::class.java, name, extension)

inline val ExtensionAware.extraProperties: ExtraPropertiesExtension
    get() = extensions.extraProperties

// TODO(Dmitrii Krasnov): we can remove this, when downloading konan from maven local will be possible KT-63198
internal fun disableDownloadingKonanFromMavenCentral(project: Project) {
    project.extraProperties.set("kotlin.native.distribution.downloadFromMaven", "false")
}

fun Project.applyKotlinJvmPlugin() {
    project.plugins.apply(KotlinPluginWrapper::class.java)
}

fun Project.applyKotlinComposePlugin() {
    project.plugins.apply(ComposeCompilerGradleSubplugin::class.java)
}

fun Project.applyMultiplatformPlugin(): KotlinMultiplatformExtension {
    addBuildEventsListenerRegistryMock(this)
    disableLegacyWarning(project)
    plugins.apply("kotlin-multiplatform")
    return extensions.getByName("kotlin") as KotlinMultiplatformExtension
}

internal fun disableLegacyWarning(project: Project) {
    project.extraProperties.set("kotlin.js.compiler.nowarn", "true")
}